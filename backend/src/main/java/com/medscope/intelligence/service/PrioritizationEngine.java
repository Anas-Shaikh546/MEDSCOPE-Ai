package com.medscope.intelligence.service;

import com.medscope.intelligence.dto.HistoricalObservation;
import com.medscope.intelligence.dto.IntelligenceContext;
import com.medscope.intelligence.dto.TestResultContext;
import com.medscope.intelligence.dto.TestTrendContext;
import com.medscope.intelligence.entity.InsightPriority;
import com.medscope.intelligence.entity.InsightType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic rule-based prioritization engine. No AI involved -
 * pure logic using structured evidence from the context.
 *
 * See step7.txt Task 3: "Before calling AI, create deterministic rules.
 * This is important because not everything needs AI."
 *
 * Rules flag which patterns deserve attention based on:
 * - Current abnormality vs reference range
 * - Persistent abnormality across multiple reports
 * - Significant change between observations
 * - Trend direction (from Step 6's TrendCalculator)
 */
@Component
@Slf4j
public class PrioritizationEngine {

    /**
     * Evaluate the intelligence context and generate deterministic flags.
     * The AI receives these flags along with the context to generate
     * natural-language insights.
     */
    public List<PrioritizationFlag> evaluate(IntelligenceContext context) {
        List<PrioritizationFlag> flags = new ArrayList<>();

        // Rule 1: Current abnormalities (outside reference range).
        flags.addAll(evaluateCurrentAbnormalities(context));

        // Rule 2: Persistent abnormalities (same test abnormal across multiple reports).
        flags.addAll(evaluatePersistentAbnormalities(context));

        // Rule 3: Significant trends (INCREASING/DECREASING with multiple observations).
        flags.addAll(evaluateSignificantTrends(context));

        log.debug("Generated {} prioritization flags for report {}",
                flags.size(), context.getReportId());

        return flags;
    }

    private List<PrioritizationFlag> evaluateCurrentAbnormalities(IntelligenceContext context) {
        List<PrioritizationFlag> flags = new ArrayList<>();

        for (TestResultContext result : context.getCurrentResults()) {
            if (result.getNumericValue() == null) {
                continue; // Only evaluate numeric results.
            }

            if (result.getReferenceLow() == null && result.getReferenceHigh() == null) {
                continue; // No reference range provided.
            }

            boolean isAbnormal = false;
            String abnormalityType = null;

            if (result.getReferenceLow() != null && result.getNumericValue() < result.getReferenceLow()) {
                isAbnormal = true;
                abnormalityType = "BELOW_RANGE";
            } else if (result.getReferenceHigh() != null && result.getNumericValue() > result.getReferenceHigh()) {
                isAbnormal = true;
                abnormalityType = "ABOVE_RANGE";
            }

            if (isAbnormal) {
                InsightPriority priority = determinePriority(result, abnormalityType);

                flags.add(PrioritizationFlag.builder()
                        .type(InsightType.SIGNIFICANT_CHANGE)
                        .priority(priority)
                        .testName(result.getCanonicalName())
                        .sourceResultIds(List.of(result.getResultId()))
                        .evidence("Current value " + result.getNumericValue() + " " + result.getUnit() +
                                " is " + abnormalityType.toLowerCase().replace("_", " ") +
                                " (reference: " + formatRange(result.getReferenceLow(), result.getReferenceHigh()) + ")")
                        .build());
            }
        }

        return flags;
    }

    private List<PrioritizationFlag> evaluatePersistentAbnormalities(IntelligenceContext context) {
        List<PrioritizationFlag> flags = new ArrayList<>();

        for (TestTrendContext trend : context.getHistoricalTrends()) {
            if (trend.getObservations().size() < 3) {
                continue; // Need at least 3 observations to flag persistence.
            }

            int abnormalCount = 0;
            List<Long> abnormalResultIds = new ArrayList<>();

            for (HistoricalObservation obs : trend.getObservations()) {
                if (obs.getValue() == null) {
                    continue;
                }

                boolean isAbnormal = false;
                if (obs.getReferenceLow() != null && obs.getValue() < obs.getReferenceLow()) {
                    isAbnormal = true;
                } else if (obs.getReferenceHigh() != null && obs.getValue() > obs.getReferenceHigh()) {
                    isAbnormal = true;
                }

                if (isAbnormal) {
                    abnormalCount++;
                    abnormalResultIds.add(obs.getReportResultId());
                }
            }

            // Flag if at least 3 abnormal observations.
            if (abnormalCount >= 3) {
                flags.add(PrioritizationFlag.builder()
                        .type(InsightType.PERSISTENT_ABNORMALITY)
                        .priority(InsightPriority.MODERATE)
                        .testName(trend.getCanonicalName())
                        .sourceResultIds(abnormalResultIds)
                        .evidence(trend.getDisplayName() + " has remained outside reference range across " +
                                abnormalCount + " reports")
                        .build());
            }
        }

        return flags;
    }

    private List<PrioritizationFlag> evaluateSignificantTrends(IntelligenceContext context) {
        List<PrioritizationFlag> flags = new ArrayList<>();

        for (TestTrendContext trend : context.getHistoricalTrends()) {
            if (trend.getObservations().size() < 3) {
                continue; // Need at least 3 observations for a meaningful trend.
            }

            // TrendDirection from Step 6's TrendCalculator (deterministic).
            String direction = trend.getTrendDirection();
            if ("INCREASING".equals(direction) || "DECREASING".equals(direction)) {
                List<Long> sourceIds = trend.getObservations().stream()
                        .map(HistoricalObservation::getReportResultId)
                        .toList();

                flags.add(PrioritizationFlag.builder()
                        .type(InsightType.TREND_CONTEXT)
                        .priority(InsightPriority.LOW)
                        .testName(trend.getCanonicalName())
                        .sourceResultIds(sourceIds)
                        .evidence(trend.getDisplayName() + " shows " + direction.toLowerCase() +
                                " trend across " + trend.getObservations().size() + " observations")
                        .build());
            }
        }

        return flags;
    }

    private InsightPriority determinePriority(TestResultContext result, String abnormalityType) {
        if (result.getReferenceLow() == null || result.getReferenceHigh() == null) {
            return InsightPriority.MODERATE;
        }

        double value = result.getNumericValue();
        double range = result.getReferenceHigh() - result.getReferenceLow();

        if ("BELOW_RANGE".equals(abnormalityType)) {
            double deviation = result.getReferenceLow() - value;
            // High priority if more than 30% below the range.
            return (deviation > range * 0.3) ? InsightPriority.HIGH : InsightPriority.MODERATE;
        } else if ("ABOVE_RANGE".equals(abnormalityType)) {
            double deviation = value - result.getReferenceHigh();
            // High priority if more than 30% above the range.
            return (deviation > range * 0.3) ? InsightPriority.HIGH : InsightPriority.MODERATE;
        }

        return InsightPriority.MODERATE;
    }

    private String formatRange(Double low, Double high) {
        if (low != null && high != null) {
            return low + "-" + high;
        } else if (low != null) {
            return ">= " + low;
        } else if (high != null) {
            return "<= " + high;
        }
        return "unknown";
    }
}
