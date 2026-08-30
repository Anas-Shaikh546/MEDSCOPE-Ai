package com.medscope.interpretation.dto;

import com.medscope.interpretation.entity.Analysis;
import com.medscope.interpretation.entity.AnalysisStatus;

import java.time.Instant;
import java.util.List;

/** API response for a persisted interpretation, not the FastAPI response. */
public record AnalysisResponse(
        Long id,
        Long reportId,
        AnalysisStatus status,
        String summary,
        String recommendations,
        String modelName,
        String modelVersion,
        String promptVersion,
        Instant createdAt,
        List<AnalysisFindingResponse> findings
) {
    public static AnalysisResponse from(Analysis analysis, List<AnalysisFindingResponse> findings) {
        return new AnalysisResponse(
                analysis.getId(),
                analysis.getReportId(),
                analysis.getStatus(),
                analysis.getSummary(),
                analysis.getRecommendations(),
                analysis.getModelName(),
                analysis.getModelVersion(),
                analysis.getPromptVersion(),
                analysis.getCreatedAt(),
                findings
        );
    }
}
