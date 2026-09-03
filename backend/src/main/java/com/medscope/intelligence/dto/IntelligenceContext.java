package com.medscope.intelligence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured context assembled from ReportResult + Analysis + Timeline
 * data, ready to be sent to the AI. The AI never queries the database
 * directly - it receives only this pre-assembled, validated context.
 *
 * See step7.txt Task 2: "IntelligenceContext should contain: Current
 * report, Historical results, Current abnormalities, Historical
 * abnormalities, Trends, Reference ranges, Dates, Units, Existing
 * interpretation."
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntelligenceContext {

    private Long reportId;
    private String reportDate;

    // Current report's extracted facts.
    private List<TestResultContext> currentResults;

    // Historical observations grouped by test, with trend direction.
    private List<TestTrendContext> historicalTrends;

    // Existing Step 5 AI interpretation (summary + findings).
    private String existingAnalysisSummary;
    private String existingRecommendations;
}
