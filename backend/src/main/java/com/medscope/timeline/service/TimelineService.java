package com.medscope.timeline.service;

import com.medscope.timeline.dto.TestTrendDto;
import com.medscope.timeline.dto.TimelineObservationDto;
import com.medscope.timeline.dto.TrendsResponse;
import com.medscope.timeline.entity.TestDefinition;
import com.medscope.timeline.repository.TestDefinitionRepository;
import com.medscope.timeline.repository.TimelineRepository;
import com.medscope.timeline.trend.TrendCalculator;
import com.medscope.timeline.trend.TrendDirection;
import com.medscope.timeline.trend.TrendObservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the timeline/trend layer. Never writes to Step 4 or
 * Step 5 tables (analysis, interpretation packages are frozen from
 * this layer's perspective - it only reads them via TimelineRepository).
 *
 * Ownership is enforced by every query being scoped to authenticatedUserId
 * from the JWT (never a client-supplied id) - same pattern as every
 * other per-user resource in this codebase (6.17).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineService {

    private final TimelineRepository timelineRepository;
    private final TestDefinitionRepository testDefinitionRepository;
    private final TrendCalculator trendCalculator;

    /**
     * All trends for the authenticated user - one entry per canonical
     * test name for which they have at least one numeric result.
     */
    public TrendsResponse getAllTrends(Long userId) {
        List<String> canonicalNames = timelineRepository.findDistinctCanonicalNamesForUser(userId);

        List<TestTrendDto> trends = canonicalNames.stream()
                .map(name -> buildTrend(userId, name))
                .flatMap(Optional::stream)
                .toList();

        return new TrendsResponse(trends);
    }

    /**
     * Trend for one specific canonical test name.
     * Returns empty Optional if the user has no results for this test.
     */
    public Optional<TestTrendDto> getTrendForTest(Long userId, String canonicalName) {
        return buildTrend(userId, canonicalName);
    }

    private Optional<TestTrendDto> buildTrend(Long userId, String canonicalName) {
        List<Object[]> rows = timelineRepository.findObservationsForUserAndTest(userId, canonicalName);

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        // Map raw query rows to typed objects.
        List<TimelineObservationDto> observationDtos = new ArrayList<>();
        List<TrendObservation> trendInputs = new ArrayList<>();

        for (Object[] row : rows) {
            Long reportId       = ((Number) row[0]).longValue();
            Long reportResultId = ((Number) row[1]).longValue();
            double value        = ((Number) row[2]).doubleValue();
            String unit         = (String) row[3];
            LocalDate testDate  = (LocalDate) row[4];  // nullable
            Instant createdAt   = (Instant) row[5];
            Double refLow       = row[6] != null ? ((Number) row[6]).doubleValue() : null;
            Double refHigh      = row[7] != null ? ((Number) row[7]).doubleValue() : null;
            String status       = row[8] != null ? row[8].toString() : null;

            // Fallback ordering: confirmed test date > upload date.
            // The DTO carries dateIsConfirmed so the UI can label the
            // distinction clearly rather than implying a confirmed lab
            // date when only an upload date is available (6.2, Test 10).
            boolean dateIsConfirmed = testDate != null;
            LocalDate resolvedDate = dateIsConfirmed
                    ? testDate
                    : createdAt.atZone(ZoneOffset.UTC).toLocalDate();

            Instant orderedAt = dateIsConfirmed
                    ? testDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                    : createdAt;

            observationDtos.add(new TimelineObservationDto(
                    resolvedDate, dateIsConfirmed,
                    reportId, reportResultId,
                    value, unit, refLow, refHigh, status
            ));

            if (unit != null && !unit.isBlank()) {
                trendInputs.add(new TrendObservation(value, unit, orderedAt));
            }
        }

        // Sort observations by resolved date for display (query returned
        // unsorted since we handled fallback in Java).
        observationDtos.sort((a, b) -> a.date().compareTo(b.date()));

        TrendDirection direction = trendCalculator.calculate(trendInputs);

        // Look up display metadata from TestDefinition - null-safe,
        // since a normalizedTestName that exists in report_results may
        // theoretically not have a matching test_definition row if the
        // seed data ever drifts from the vocabulary (flagged in seed
        // count test).
        TestDefinition def = testDefinitionRepository
                .findByCanonicalName(canonicalName)
                .orElse(null);

        String displayName = def != null ? def.getDisplayName() : canonicalName;
        String category    = def != null ? def.getCategory().name() : "OTHER";
        String defaultUnit = def != null ? def.getDefaultUnit() : null;

        return Optional.of(new TestTrendDto(
                canonicalName, displayName, category, defaultUnit,
                direction, observationDtos
        ));
    }
}