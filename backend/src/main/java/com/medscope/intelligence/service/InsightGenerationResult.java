package com.medscope.intelligence.service;

import com.medscope.intelligence.entity.Insight;
import com.medscope.intelligence.entity.InsightGeneration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of intelligence generation operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightGenerationResult {

    private boolean success;
    private String errorMessage;
    private InsightGeneration generation;
    private List<Insight> insights;

    public static InsightGenerationResult success(InsightGeneration generation, List<Insight> insights) {
        return InsightGenerationResult.builder()
                .success(true)
                .generation(generation)
                .insights(insights)
                .build();
    }

    public static InsightGenerationResult failed(String errorMessage) {
        return InsightGenerationResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    public static InsightGenerationResult notFound() {
        return InsightGenerationResult.builder()
                .success(false)
                .errorMessage("Report not found or access denied")
                .build();
    }
}
