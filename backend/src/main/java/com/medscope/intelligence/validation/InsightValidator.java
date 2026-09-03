package com.medscope.intelligence.validation;

import com.medscope.analysis.repository.ReportResultRepository;
import com.medscope.intelligence.client.GeneratedInsightDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates AI-generated insights before persistence. Critical safety layer
 * that prevents fabricated source IDs from being saved.
 *
 * See step7.txt section 7: "If AI returns sourceResultIds: [999999] and
 * result 999999 doesn't exist: REJECT. Do not blindly save it."
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InsightValidator {

    private final ReportResultRepository reportResultRepository;

    /**
     * Validate a list of AI-generated insights. Returns only valid insights.
     * Invalid insights are logged and rejected.
     */
    public List<GeneratedInsightDto> validate(Long reportId, List<GeneratedInsightDto> insights) {
        List<GeneratedInsightDto> validated = new ArrayList<>();

        for (GeneratedInsightDto insight : insights) {
            if (isValid(reportId, insight)) {
                validated.add(insight);
            } else {
                log.warn("Rejected invalid insight for report {}: {}", reportId, insight.getTitle());
            }
        }

        log.debug("Validated {}/{} insights for report {}", validated.size(), insights.size(), reportId);
        return validated;
    }

    private boolean isValid(Long reportId, GeneratedInsightDto insight) {
        // Rule 1: Must have at least one source result ID
        if (insight.getSourceResultIds() == null || insight.getSourceResultIds().isEmpty()) {
            log.warn("Insight rejected: no source result IDs (title: {})", insight.getTitle());
            return false;
        }

        // Rule 2: Every source result ID must exist in the database
        for (Long sourceId : insight.getSourceResultIds()) {
            if (!reportResultRepository.existsById(sourceId)) {
                log.warn("Insight rejected: fabricated source result ID {} (title: {})",
                        sourceId, insight.getTitle());
                return false;
            }
        }

        // Rule 3: Title and description must be present
        if (insight.getTitle() == null || insight.getTitle().isBlank()) {
            log.warn("Insight rejected: missing title");
            return false;
        }

        if (insight.getDescription() == null || insight.getDescription().isBlank()) {
            log.warn("Insight rejected: missing description (title: {})", insight.getTitle());
            return false;
        }

        // Rule 4: Type and priority must be set
        if (insight.getType() == null || insight.getPriority() == null) {
            log.warn("Insight rejected: missing type or priority (title: {})", insight.getTitle());
            return false;
        }

        return true;
    }
}
