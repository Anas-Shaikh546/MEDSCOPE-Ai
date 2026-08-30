package com.medscope.interpretation;

import com.medscope.analysis.entity.ReportResult;
import com.medscope.analysis.entity.ResultStatus;
import com.medscope.analysis.repository.ReportResultRepository;
import com.medscope.interpretation.client.AiServiceClient;
import com.medscope.interpretation.dto.AnalysisFindingDto;
import com.medscope.interpretation.dto.AnalyzeResponse;
import com.medscope.report.entity.Report;
import com.medscope.report.entity.ReportStatus;
import com.medscope.report.repository.ReportRepository;
import com.medscope.security.JwtService;
import com.medscope.user.entity.User;
import com.medscope.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies Task 4's HTTP contract through the real security filter chain. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalysisControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private ReportResultRepository reportResultRepository;
    @MockBean private AiServiceClient aiServiceClient;

    @Test
    void ownerCanCreateAndReadAnInterpretation() throws Exception {
        User owner = userRepository.save(User.builder()
                .email("controller-analysis-owner@example.com").passwordHash("hash")
                .firstName("Anas").lastName("Shaikh").build());
        Report report = reportRepository.save(Report.builder()
                .userId(owner.getId()).originalFilename("report.pdf")
                .storedFilename(UUID.randomUUID() + ".pdf").filePath("test/report.pdf")
                .contentType("application/pdf").fileSize(100L).status(ReportStatus.PROCESSED).build());
        ReportResult result = reportResultRepository.save(ReportResult.builder()
                .reportId(report.getId()).testName("Hemoglobin").rawValue("13.8")
                .numericValue(13.8).status(ResultStatus.NORMAL).confidence(0.95).build());

        AnalyzeResponse aiResponse = new AnalyzeResponse();
        aiResponse.setStatus("COMPLETED");
        aiResponse.setSummary("Within range.");
        aiResponse.setRecommendations("Routine follow-up.");
        aiResponse.setFindings(List.of(new AnalysisFindingDto(0, "Within the provided range.", "NORMAL")));
        aiResponse.setModelName("nvidia/nemotron-3-ultra-550b-a55b:free");
        aiResponse.setModelVersion("nvidia-nemotron-3-ultra-550b");
        aiResponse.setPromptVersion("v1.0");
        when(aiServiceClient.analyze(any())).thenReturn(aiResponse);

        String token = jwtService.generateToken(owner.getId(), owner.getEmail());
        String createResponse = mockMvc.perform(post("/interpretations/analyze/" + report.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(report.getId()))
                .andExpect(jsonPath("$.findings[0].reportResultId").value(result.getId()))
                .andReturn().getResponse().getContentAsString();

        long analysisId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/interpretations/" + analysisId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Within range."));
        mockMvc.perform(get("/interpretations/by-report/" + report.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(analysisId));
    }
}
