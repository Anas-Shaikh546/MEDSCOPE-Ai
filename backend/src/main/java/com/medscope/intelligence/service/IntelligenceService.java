package com.medscope.intelligence.service;

import com.medscope.intelligence.client.FastApiIntelligenceClient;
import com.medscope.intelligence.client.GeneratedInsightDto;
import com.medscope.intelligence.client.IntelligenceResponse;
import com.medscope.intelligence.dto.IntelligenceContext;
import com.medscope.intelligence.dto.InsightDto;
import com.medscope.intelligence.entity.*;
import com.medscope.intelligence.repository.InsightGenerationRepository;
import com.medscope.intelligence.repository.InsightRepository;
import com.medscope.intelligence.repository.InsightSourceRepository;
import com.medscope.intelligence.validation.InsightValidator;
import com.medscope.report.entity.Report;
import com.medscope.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates intelligence generation: context → rules → AI → validation → persist.
 * Follows the pattern from AnalysisService (Step 5) but for longitudinal insights.
 *
 * See step7.txt Task 5: "IntelligenceService orchestration — context → rules →
 * AI → validation → persist. Handle ownership, idempotency, AI failures."
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntelligenceService {

    private final ReportRepository reportRepository;
    private final InsightGenerationRepository generationRepository;
    private final InsightRepository insightRepository;
    private final InsightSourceRepository sourceRepository;
    private final ContextBuilderService contextBuilder;
    private final PrioritizationEngine prioritizationEngine;
    private final FastApiIntelligenceClient aiClient;
    private final InsightValidator validator;

    /**
     * Generate intelligence for a report. Idempotent - replaces existing
     * insights on regeneration (same delete-then-insert pattern as Step 5).
     */
    @Transactional
    public InsightGenerationResult generateInsights(Long userId, Long reportId) {
        log.info("Generating insights for user {} report {}", userId, reportId);

        // Ownership check
        Optional<Report> reportOpt = reportRepository.findByIdAndUserId(reportId, userId);
        if (reportOpt.isEmpty()) {
            log.warn("Report {} not found or not owned by user {}", reportId, userId);
            return InsightGenerationResult.notFound();
        }

        Report report = reportOpt.get();

        // Step 1: Build context
        Optional<IntelligenceContext> contextOpt = contextBuilder.buildContext(userId, reportId);
        if (contextOpt.isEmpty()) {
            log.warn("Could not build intelligence context for report {}", reportId);
            return InsightGenerationResult.failed("Could not build context");
        }

        IntelligenceContext context = contextOpt.get();

        // Step 2: Run deterministic rules
        List<PrioritizationFlag> flags = prioritizationEngine.evaluate(context);
        log.debug("Generated {} prioritization flags for report {}", flags.size(), reportId);

        // Step 3: Call AI
        IntelligenceResponse aiResponse = aiClient.generateInsights(context, flags);

        if (!"COMPLETED".equals(aiResponse.getStatus())) {
            log.error("AI intelligence generation failed for report {}: {}",
                    reportId, aiResponse.getErrorMessage());
            return createFailedGeneration(reportId, aiResponse);
        }

        // Step 4: Validate AI output
        List<GeneratedInsightDto> validatedInsights = validator.validate(reportId, aiResponse.getInsights());

        if (validatedInsights.isEmpty()) {
            log.warn("No valid insights generated for report {}", reportId);
            return createEmptyGeneration(reportId, aiResponse);
        }

        // Step 5: Persist (idempotent - replace existing)
        return persistInsights(reportId, validatedInsights, aiResponse);
    }

    /**
     * Get all insights for a user, grouped by generation.
     */
    public List<InsightGenerationSummary> getAllInsights(Long userId) {
        // Get all reports for user
        List<Report> reports = reportRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        List<Long> reportIds = reports.stream().map(Report::getId).toList();

        if (reportIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Get latest generation for each report
        List<InsightGenerationSummary> summaries = new ArrayList<>();
        for (Long reportId : reportIds) {
            Optional<InsightGeneration> latestGen = generationRepository
                    .findTopByReportIdOrderByGenerationNumberDesc(reportId);

            if (latestGen.isPresent()) {
                InsightGeneration gen = latestGen.get();
                List<Insight> insights = insightRepository.findAllByGenerationId(gen.getId());

                summaries.add(InsightGenerationSummary.builder()
                        .generationId(gen.getId())
                        .reportId(reportId)
                        .generationNumber(gen.getGenerationNumber())
                        .status(gen.getStatus())
                        .insightCount(insights.size())
                        .createdAt(gen.getCreatedAt().toString())
                        .insights(insights.stream().map(this::toDto).toList())
                        .build());
            }
        }

        return summaries;
    }

    /**
     * Get insights for a specific report (latest generation).
     */
    public Optional<InsightGenerationSummary> getInsightsForReport(Long userId, Long reportId) {
        // Ownership check
        if (reportRepository.findByIdAndUserId(reportId, userId).isEmpty()) {
            return Optional.empty();
        }

        Optional<InsightGeneration> latestGen = generationRepository
                .findTopByReportIdOrderByGenerationNumberDesc(reportId);

        if (latestGen.isEmpty()) {
            return Optional.empty();
        }

        InsightGeneration gen = latestGen.get();
        List<Insight> insights = insightRepository.findAllByGenerationId(gen.getId());

        return Optional.of(InsightGenerationSummary.builder()
                .generationId(gen.getId())
                .reportId(reportId)
                .generationNumber(gen.getGenerationNumber())
                .status(gen.getStatus())
                .insightCount(insights.size())
                .createdAt(gen.getCreatedAt().toString())
                .insights(insights.stream().map(this::toDto).toList())
                .build());
    }

    private InsightDto toDto(Insight insight) {
        return InsightDto.builder()
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
                .build();
    }

    private InsightGenerationResult persistInsights(
            Long reportId,
            List<GeneratedInsightDto> validatedInsights,
            IntelligenceResponse aiResponse
    ) {
        // Find or create generation record
        Optional<InsightGeneration> existingGen = generationRepository
                .findTopByReportIdOrderByGenerationNumberDesc(reportId);

        int nextGenerationNumber = existingGen.map(g -> g.getGenerationNumber() + 1).orElse(1);

        // Delete old insights if regenerating (idempotency)
        if (existingGen.isPresent()) {
            InsightGeneration oldGen = existingGen.get();
            List<Insight> oldInsights = insightRepository.findAllByGenerationId(oldGen.getId());
            for (Insight oldInsight : oldInsights) {
                sourceRepository.deleteAllByInsightId(oldInsight.getId());
            }
            insightRepository.deleteAllByGenerationId(oldGen.getId());
            log.debug("Deleted {} old insights for report {}", oldInsights.size(), reportId);
        }

        // Create new generation record
        InsightGeneration generation = InsightGeneration.builder()
                .reportId(reportId)
                .generationNumber(nextGenerationNumber)
                .status(InsightGenerationStatus.COMPLETED)
                .modelName(aiResponse.getModelName())
                .modelVersion(aiResponse.getModelVersion())
                .promptVersion(aiResponse.getPromptVersion())
                .build();
        generation = generationRepository.save(generation);

        // Create insights
        List<Insight> savedInsights = new ArrayList<>();
        for (GeneratedInsightDto dto : validatedInsights) {
            Insight insight = Insight.builder()
                    .generationId(generation.getId())
                    .type(dto.getType())
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .priority(dto.getPriority())
                    .confidence(dto.getConfidence())
                    .followUpQuestions(dto.getFollowUpQuestions())
                    .status(InsightStatus.GENERATED)
                    .build();
            insight = insightRepository.save(insight);

            // Create sources
            for (Long sourceResultId : dto.getSourceResultIds()) {
                InsightSource source = InsightSource.builder()
                        .insightId(insight.getId())
                        .reportResultId(sourceResultId)
                        .build();
                sourceRepository.save(source);
            }

            savedInsights.add(insight);
        }

        log.info("Persisted {} insights for report {} (generation {})",
                savedInsights.size(), reportId, nextGenerationNumber);

        return InsightGenerationResult.success(generation, savedInsights);
    }

    private InsightGenerationResult createFailedGeneration(Long reportId, IntelligenceResponse aiResponse) {
        InsightGeneration generation = InsightGeneration.builder()
                .reportId(reportId)
                .generationNumber(1)
                .status(InsightGenerationStatus.FAILED)
                .modelName(aiResponse.getModelName())
                .modelVersion(aiResponse.getModelVersion())
                .promptVersion(aiResponse.getPromptVersion())
                .build();
        generation = generationRepository.save(generation);

        return InsightGenerationResult.failed(aiResponse.getErrorMessage());
    }

    private InsightGenerationResult createEmptyGeneration(Long reportId, IntelligenceResponse aiResponse) {
        InsightGeneration generation = InsightGeneration.builder()
                .reportId(reportId)
                .generationNumber(1)
                .status(InsightGenerationStatus.COMPLETED)
                .modelName(aiResponse.getModelName())
                .modelVersion(aiResponse.getModelVersion())
                .promptVersion(aiResponse.getPromptVersion())
                .build();
        generation = generationRepository.save(generation);

        return InsightGenerationResult.success(generation, new ArrayList<>());
    }
}
