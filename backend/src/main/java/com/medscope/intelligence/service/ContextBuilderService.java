package com.medscope.intelligence.service;

import com.medscope.analysis.entity.ReportResult;
import com.medscope.analysis.repository.ReportResultRepository;
import com.medscope.intelligence.dto.*;
import com.medscope.interpretation.entity.Analysis;
import com.medscope.interpretation.repository.AnalysisRepository;
import com.medscope.report.entity.Report;
import com.medscope.report.repository.ReportRepository;
import com.medscope.timeline.dto.TestTrendDto;
import com.medscope.timeline.dto.TimelineObservationDto;
import com.medscope.timeline.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Assembles IntelligenceContext from Step 4/5/6 data. Pure data assembly -
 * no AI calls, no business logic, no rule evaluation. The AI receives only
 * this structured context, never raw database queries.
 *
 * See step7.txt Task 2: "The AI should not directly query your database.
 * Instead: Database → Context Builder → Structured IntelligenceContext → AI"
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContextBuilderService {

    private final ReportRepository reportRepository;
    private final ReportResultRepository reportResultRepository;
    private final AnalysisRepository analysisRepository;
    private final TimelineService timelineService;

    /**
     * Build complete intelligence context for a report. Returns empty if
     * the report doesn't exist or doesn't belong to the user (ownership
     * enforced at the repository layer).
     */
    public Optional<IntelligenceContext> buildContext(Long userId, Long reportId) {
        log.debug("Building intelligence context for user {} report {}", userId, reportId);

        // Verify report exists and belongs to user.
        Optional<Report> reportOpt = reportRepository.findByIdAndUserId(reportId, userId);
        if (reportOpt.isEmpty()) {
            log.warn("Report {} not found for user {}", reportId, userId);
            return Optional.empty();
        }

        Report report = reportOpt.get();

        // Current report's extracted facts (Step 4).
        List<ReportResult> currentResults = reportResultRepository.findAllByReportIdOrderById(reportId);
        List<TestResultContext> currentResultContexts = currentResults.stream()
                .map(this::toTestResultContext)
                .toList();

        // Historical trends (Step 6).
        List<TestTrendContext> historicalTrends = timelineService.getAllTrends(userId)
                .trends()
                .stream()
                .map(this::toTestTrendContext)
                .toList();

        // Existing Step 5 interpretation.
        Analysis analysis = analysisRepository.findByReportId(reportId).orElse(null);
        String existingSummary = analysis != null ? analysis.getSummary() : null;
        String existingRecommendations = analysis != null ? analysis.getRecommendations() : null;

        IntelligenceContext context = IntelligenceContext.builder()
                .reportId(reportId)
                .reportDate(report.getTestDate() != null ? report.getTestDate().toString() : null)
                .currentResults(currentResultContexts)
                .historicalTrends(historicalTrends)
                .existingAnalysisSummary(existingSummary)
                .existingRecommendations(existingRecommendations)
                .build();

        log.debug("Built context with {} current results, {} trends",
                currentResultContexts.size(), historicalTrends.size());

        return Optional.of(context);
    }

    private TestResultContext toTestResultContext(ReportResult result) {
        return TestResultContext.builder()
                .resultId(result.getId())
                .testName(result.getTestName())
                .canonicalName(result.getNormalizedTestName())
                .numericValue(result.getNumericValue())
                .textValue(result.getTextValue())
                .unit(result.getUnit())
                .referenceLow(result.getReferenceLow())
                .referenceHigh(result.getReferenceHigh())
                .status(result.getStatus() != null ? result.getStatus().name() : null)
                .build();
    }

    private TestTrendContext toTestTrendContext(TestTrendDto trend) {
        List<HistoricalObservation> observations = trend.observations().stream()
                .map(this::toHistoricalObservation)
                .toList();

        return TestTrendContext.builder()
                .canonicalName(trend.canonicalName())
                .displayName(trend.displayName())
                .trendDirection(trend.trend().name())
                .observations(observations)
                .build();
    }

    private HistoricalObservation toHistoricalObservation(TimelineObservationDto obs) {
        return HistoricalObservation.builder()
                .date(obs.date().toString())
                .dateIsConfirmed(obs.dateIsConfirmed())
                .reportResultId(obs.reportResultId())
                .value(obs.value())
                .unit(obs.unit())
                .referenceLow(obs.referenceLow())
                .referenceHigh(obs.referenceHigh())
                .status(obs.status())
                .build();
    }
}
