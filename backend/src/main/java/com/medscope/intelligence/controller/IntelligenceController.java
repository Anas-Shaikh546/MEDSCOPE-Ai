package com.medscope.intelligence.controller;

import com.medscope.intelligence.dto.InsightDto;
import com.medscope.intelligence.service.IntelligenceService;
import com.medscope.intelligence.service.InsightGenerationResult;
import com.medscope.intelligence.service.InsightGenerationSummary;
import com.medscope.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for intelligence generation and retrieval.
 * Ownership enforced via @CurrentUser (same pattern as other controllers).
 */
@RestController
@RequestMapping("/insights")
@RequiredArgsConstructor
public class IntelligenceController {

    private final IntelligenceService intelligenceService;

    /**
     * Generate insights for a report.
     * POST /api/insights/reports/{reportId}/generate
     */
    @PostMapping("/reports/{reportId}/generate")
    public ResponseEntity<?> generateInsights(
            @CurrentUser Long authenticatedUserId,
            @PathVariable Long reportId
    ) {
        InsightGenerationResult result = intelligenceService.generateInsights(authenticatedUserId, reportId);

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }

        return ResponseEntity.ok(InsightGenerationSummary.builder()
                .generationId(result.getGeneration().getId())
                .reportId(reportId)
                .generationNumber(result.getGeneration().getGenerationNumber())
                .status(result.getGeneration().getStatus())
                .insightCount(result.getInsights().size())
                .createdAt(result.getGeneration().getCreatedAt().toString())
                .insights(result.getInsights().stream()
                        .map(insight -> InsightDto.builder()
                                .id(insight.getId())
                                .generationId(insight.getGenerationId())
                                .type(insight.getType())
                                .title(insight.getTitle())
                                .description(insight.getDescription())
                                .priority(insight.getPriority())
                                .followUpQuestions(insight.getFollowUpQuestions())
                                .confidence(insight.getConfidence())
                                .status(insight.getStatus())
                                .createdAt(insight.getCreatedAt().toString())
                                .build())
                        .toList())
                .build());
    }

    /**
     * Get all insights for authenticated user.
     * GET /api/insights
     */
    @GetMapping
    public ResponseEntity<List<InsightGenerationSummary>> getAllInsights(
            @CurrentUser Long authenticatedUserId
    ) {
        List<InsightGenerationSummary> summaries = intelligenceService.getAllInsights(authenticatedUserId);
        return ResponseEntity.ok(summaries);
    }

    /**
     * Get insights for a specific report.
     * GET /api/insights/reports/{reportId}
     */
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<?> getInsightsForReport(
            @CurrentUser Long authenticatedUserId,
            @PathVariable Long reportId
    ) {
        return intelligenceService.getInsightsForReport(authenticatedUserId, reportId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
