package com.medscope.interpretation;

import com.medscope.analysis.entity.ReportResult;
import com.medscope.analysis.entity.ResultStatus;
import com.medscope.analysis.repository.ReportResultRepository;
import com.medscope.common.exception.BadRequestException;
import com.medscope.common.exception.ResourceNotFoundException;
import com.medscope.interpretation.client.AiServiceClient;
import com.medscope.interpretation.dto.AnalysisFindingDto;
import com.medscope.interpretation.dto.AnalysisResponse;
import com.medscope.interpretation.dto.AnalyzeResponse;
import com.medscope.interpretation.entity.AnalysisStatus;
import com.medscope.interpretation.repository.AnalysisFindingRepository;
import com.medscope.interpretation.repository.AnalysisRepository;
import com.medscope.interpretation.service.AnalysisService;
import com.medscope.report.entity.Report;
import com.medscope.report.entity.ReportStatus;
import com.medscope.report.repository.ReportRepository;
import com.medscope.user.entity.User;
import com.medscope.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Covers Task 4's service boundary against the real H2/Flyway schema.
 * The HTTP client is mocked because its own behavior was tested in Task 3.
 */
@SpringBootTest
@ActiveProfiles("test")
class AnalysisServiceIntegrationTest {

    @Autowired private AnalysisService analysisService;
    @Autowired private UserRepository userRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private ReportResultRepository reportResultRepository;
    @Autowired private AnalysisRepository analysisRepository;
    @Autowired private AnalysisFindingRepository analysisFindingRepository;
    @MockBean private AiServiceClient aiServiceClient;

    private User user(String email) {
        return userRepository.save(User.builder()
                .email(email).passwordHash("hash").firstName("Anas").lastName("Shaikh").build());
    }

    private Report report(Long userId, ReportStatus status) {
        return reportRepository.save(Report.builder()
                .userId(userId).originalFilename("report.pdf")
                .storedFilename(UUID.randomUUID() + ".pdf")
                .filePath("test/report.pdf").contentType("application/pdf")
                .fileSize(100L).status(status).build());
    }

    private ReportResult result(Long reportId, String name) {
        return reportResultRepository.save(ReportResult.builder()
                .reportId(reportId).testName(name).normalizedTestName(name.toLowerCase())
                .rawValue("13.8").numericValue(13.8).unit("g/dL")
                .referenceLow(13.0).referenceHigh(17.0).status(ResultStatus.NORMAL)
                .confidence(0.95).build());
    }

    private AnalyzeResponse completedResponse(String summary) {
        AnalyzeResponse response = new AnalyzeResponse();
        response.setStatus("COMPLETED");
        response.setSummary(summary);
        response.setRecommendations("Discuss results with a clinician.");
        response.setModelName("nvidia/nemotron-3-ultra-550b-a55b:free");
        response.setModelVersion("nvidia-nemotron-3-ultra-550b");
        response.setPromptVersion("v1.0");
        response.setFindings(List.of(new AnalysisFindingDto(0, "Within the provided range.", "NORMAL")));
        return response;
    }

    @Test
    void analyze_persistsFindingsAndGroundsThemInTheReportResult() {
        User owner = user("analysis-owner@example.com");
        Report report = report(owner.getId(), ReportStatus.PROCESSED);
        ReportResult reportResult = result(report.getId(), "Hemoglobin");
        when(aiServiceClient.analyze(any())).thenReturn(completedResponse("Results look stable."));

        AnalysisResponse response = analysisService.analyze(report.getId(), owner.getId());

        assertEquals(AnalysisStatus.COMPLETED, response.status());
        assertEquals("Results look stable.", response.summary());
        assertEquals(1, response.findings().size());
        assertEquals(reportResult.getId(), response.findings().getFirst().reportResultId());
        assertTrue(analysisRepository.findByReportId(report.getId()).isPresent());
        assertEquals(1, analysisFindingRepository.findAllByAnalysisIdOrderById(response.id()).size());
    }

    @Test
    void analyze_again_replacesExistingAnalysisAndFindings() {
        User owner = user("reanalyze-owner@example.com");
        Report report = report(owner.getId(), ReportStatus.PROCESSED);
        result(report.getId(), "Hemoglobin");
        when(aiServiceClient.analyze(any()))
                .thenReturn(completedResponse("First analysis."))
                .thenReturn(completedResponse("Replacement analysis."));

        AnalysisResponse first = analysisService.analyze(report.getId(), owner.getId());
        AnalysisResponse second = analysisService.analyze(report.getId(), owner.getId());

        assertEquals(first.id(), second.id());
        assertEquals("Replacement analysis.", second.summary());
        assertEquals(second.id(), analysisRepository.findByReportId(report.getId()).orElseThrow().getId());
        assertEquals(1, analysisFindingRepository.findAllByAnalysisIdOrderById(first.id()).size());
    }

    @Test
    void analyze_requiresProcessedReportWithResults() {
        User owner = user("unprocessed-owner@example.com");
        Report uploaded = report(owner.getId(), ReportStatus.UPLOADED);

        assertThrows(BadRequestException.class, () -> analysisService.analyze(uploaded.getId(), owner.getId()));
    }

    @Test
    void readsNeverExposeAnotherUsersAnalysis() {
        User owner = user("private-analysis-owner@example.com");
        User otherUser = user("private-analysis-other@example.com");
        Report report = report(owner.getId(), ReportStatus.PROCESSED);
        result(report.getId(), "Hemoglobin");
        when(aiServiceClient.analyze(any())).thenReturn(completedResponse("Private analysis."));
        AnalysisResponse analysis = analysisService.analyze(report.getId(), owner.getId());

        assertThrows(ResourceNotFoundException.class,
                () -> analysisService.getById(analysis.id(), otherUser.getId()));
        assertThrows(ResourceNotFoundException.class,
                () -> analysisService.getByReportId(report.getId(), otherUser.getId()));
    }

    @Test
    void invalidAiFindingCannotBePersisted() {
        User owner = user("invalid-ai-owner@example.com");
        Report report = report(owner.getId(), ReportStatus.PROCESSED);
        result(report.getId(), "Hemoglobin");
        AnalyzeResponse invalid = completedResponse("Invalid finding.");
        invalid.setFindings(List.of(new AnalysisFindingDto(5, "Invalid index.", "NORMAL")));
        when(aiServiceClient.analyze(any())).thenReturn(invalid);

        assertThrows(BadRequestException.class, () -> analysisService.analyze(report.getId(), owner.getId()));
        assertTrue(analysisRepository.findByReportId(report.getId()).isEmpty());
    }
}
