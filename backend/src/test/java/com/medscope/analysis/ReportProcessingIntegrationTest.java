package com.medscope.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end over real HTTP: upload -> process -> results, covering
 * the Step 4 definition-of-done items that Task 4 owns: ownership,
 * idempotency, unsupported-PDF handling, and the actual JSON shape.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportProcessingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String email) throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "firstName", "Anas",
                "lastName", "Shaikh",
                "email", email,
                "password", "password123"
        ));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerBody));

        String loginBody = objectMapper.writeValueAsString(
                Map.of("email", email, "password", "password123"));
        String response = mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private Long uploadReport(String token, byte[] pdfBytes) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", pdfBytes);
        String response = mockMvc.perform(multipart("/reports").file(file)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private byte[] buildReportPdf() throws IOException {
        String text = "Hemoglobin 13.8 g/dL 13.0 - 17.0";
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildBlankPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void processTextBasedReport_extractsResultAndStatusBecomesProcessed() throws Exception {
        String token = registerAndLogin("process1@example.com");
        Long reportId = uploadReport(token, buildReportPdf());

        mockMvc.perform(post("/reports/" + reportId + "/process").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"));

        mockMvc.perform(get("/reports/" + reportId + "/results").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.results[0].testName").value("Hemoglobin"))
                .andExpect(jsonPath("$.results[0].value").value(13.8))
                .andExpect(jsonPath("$.results[0].status").value("NORMAL"));
    }

    @Test
    void processingBlankScannedLikePdf_marksUnsupported_notFailed() throws Exception {
        String token = registerAndLogin("process2@example.com");
        Long reportId = uploadReport(token, buildBlankPdf());

        mockMvc.perform(post("/reports/" + reportId + "/process").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNSUPPORTED"));

        mockMvc.perform(get("/reports/" + reportId + "/results").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNSUPPORTED"))
                .andExpect(jsonPath("$.results.length()").value(0));
    }

    @Test
    void processingTwiceIsIdempotent_noDuplicateResults() throws Exception {
        String token = registerAndLogin("process3@example.com");
        Long reportId = uploadReport(token, buildReportPdf());

        mockMvc.perform(post("/reports/" + reportId + "/process").header("Authorization", "Bearer " + token));
        mockMvc.perform(post("/reports/" + reportId + "/process").header("Authorization", "Bearer " + token));

        mockMvc.perform(get("/reports/" + reportId + "/results").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(1));
    }

    @Test
    void userBCannotProcessUserAsReport_returns404() throws Exception {
        String tokenA = registerAndLogin("procowner@example.com");
        String tokenB = registerAndLogin("procother@example.com");
        Long reportId = uploadReport(tokenA, buildReportPdf());

        mockMvc.perform(post("/reports/" + reportId + "/process").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void userBCannotGetUserAsResults_returns404() throws Exception {
        String tokenA = registerAndLogin("resowner@example.com");
        String tokenB = registerAndLogin("resother@example.com");
        Long reportId = uploadReport(tokenA, buildReportPdf());
        mockMvc.perform(post("/reports/" + reportId + "/process").header("Authorization", "Bearer " + tokenA));

        mockMvc.perform(get("/reports/" + reportId + "/results").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void unprocessedReport_resultsEndpointReturnsUploadedStatusAndEmptyList() throws Exception {
        String token = registerAndLogin("unprocessed@example.com");
        Long reportId = uploadReport(token, buildReportPdf());

        mockMvc.perform(get("/reports/" + reportId + "/results").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.results.length()").value(0));
    }
}