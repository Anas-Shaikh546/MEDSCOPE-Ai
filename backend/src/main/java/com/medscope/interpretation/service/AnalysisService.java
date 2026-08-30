package com.medscope.interpretation.service;

import com.medscope.analysis.entity.ReportResult;
import com.medscope.analysis.repository.ReportResultRepository;
import com.medscope.common.exception.BadRequestException;
import com.medscope.common.exception.ResourceNotFoundException;
import com.medscope.interpretation.client.AiServiceClient;
import com.medscope.interpretation.client.ReportResultMapper;
import com.medscope.interpretation.dto.AnalysisFindingDto;
import com.medscope.interpretation.dto.AnalysisFindingResponse;
import com.medscope.interpretation.dto.AnalysisResponse;
import com.medscope.interpretation.dto.AnalyzeRequest;
import com.medscope.interpretation.dto.AnalyzeResponse;
import com.medscope.interpretation.entity.Analysis;
import com.medscope.interpretation.entity.AnalysisFinding;
import com.medscope.interpretation.entity.AnalysisSeverity;
import com.medscope.interpretation.entity.AnalysisStatus;
import com.medscope.interpretation.repository.AnalysisFindingRepository;
import com.medscope.interpretation.repository.AnalysisRepository;
import com.medscope.report.entity.Report;
import com.medscope.report.entity.ReportStatus;
import com.medscope.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Coordinates Task 4's interpretation flow. ReportResult remains the
 * source of truth; this service only persists the AI's interpretation of
 * those already-extracted rows.
 */
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final ReportService reportService;
    private final ReportResultRepository reportResultRepository;
    private final AnalysisRepository analysisRepository;
    private final AnalysisFindingRepository analysisFindingRepository;
    private final AiServiceClient aiServiceClient;

    @Transactional
    public AnalysisResponse analyze(Long reportId, Long userId) {
        Report report = reportService.getOwnedByUserOrThrow(reportId, userId);
        if (report.getStatus() != ReportStatus.PROCESSED) {
            throw new BadRequestException("Report must be processed before it can be analyzed");
        }

        List<ReportResult> results = reportResultRepository.findAllByReportIdOrderById(reportId);
        if (results.isEmpty()) {
            throw new BadRequestException("Report has no extracted results to analyze");
        }

        AnalyzeResponse aiResponse = aiServiceClient.analyze(AnalyzeRequest.builder()
                .reportId(reportId)
                .results(ReportResultMapper.toDtoList(results))
                .build());

        validateCompletedResponse(aiResponse, results.size());

        // Re-analysis updates the single row for this report and replaces its
        // findings. The database unique constraint remains the final guard.
        Analysis analysis = analysisRepository.findByReportId(reportId)
                .orElseGet(() -> Analysis.builder().reportId(reportId).build());

        if (analysis.getId() != null) {
            analysisFindingRepository.deleteAllByAnalysisId(analysis.getId());
        }

        analysis.setStatus(AnalysisStatus.COMPLETED);
        analysis.setSummary(aiResponse.getSummary());
        analysis.setRecommendations(aiResponse.getRecommendations());
        analysis.setModelName(aiResponse.getModelName());
        analysis.setModelVersion(aiResponse.getModelVersion());
        analysis.setPromptVersion(aiResponse.getPromptVersion());
        analysis = analysisRepository.saveAndFlush(analysis);
        Long analysisId = analysis.getId();

        List<AnalysisFinding> findings = aiResponse.getFindings().stream()
                .map(finding -> toEntity(analysisId, results, finding))
                .toList();
        List<AnalysisFindingResponse> responseFindings = analysisFindingRepository.saveAll(findings).stream()
                .map(AnalysisFindingResponse::from)
                .toList();

        return AnalysisResponse.from(analysis, responseFindings);
    }

    @Transactional(readOnly = true)
    public AnalysisResponse getById(Long analysisId, Long userId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found"));
        reportService.getOwnedByUserOrThrow(analysis.getReportId(), userId);
        return toResponse(analysis);
    }

    @Transactional(readOnly = true)
    public AnalysisResponse getByReportId(Long reportId, Long userId) {
        reportService.getOwnedByUserOrThrow(reportId, userId);
        Analysis analysis = analysisRepository.findByReportId(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found"));
        return toResponse(analysis);
    }

    private AnalysisResponse toResponse(Analysis analysis) {
        List<AnalysisFindingResponse> findings = analysisFindingRepository
                .findAllByAnalysisIdOrderById(analysis.getId()).stream()
                .map(AnalysisFindingResponse::from)
                .toList();
        return AnalysisResponse.from(analysis, findings);
    }

    private void validateCompletedResponse(AnalyzeResponse response, int resultCount) {
        if (!"COMPLETED".equals(response.getStatus())) {
            throw new BadRequestException("AI service did not complete the analysis");
        }

        for (AnalysisFindingDto finding : response.getFindings()) {
            if (finding == null || finding.getReportResultIndex() == null
                    || finding.getReportResultIndex() < 0
                    || finding.getReportResultIndex() >= resultCount
                    || finding.getInterpretation() == null || finding.getInterpretation().isBlank()
                    || finding.getSeverity() == null) {
                throw new BadRequestException("AI service returned an invalid finding");
            }
            try {
                AnalysisSeverity.valueOf(finding.getSeverity());
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("AI service returned an invalid finding severity");
            }
        }
    }

    private AnalysisFinding toEntity(Long analysisId, List<ReportResult> results, AnalysisFindingDto finding) {
        return AnalysisFinding.builder()
                .analysisId(analysisId)
                .reportResultId(results.get(finding.getReportResultIndex()).getId())
                .interpretation(finding.getInterpretation())
                .severity(AnalysisSeverity.valueOf(finding.getSeverity()))
                .build();
    }
}
