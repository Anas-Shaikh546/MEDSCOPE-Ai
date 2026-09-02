package com.medscope.timeline;

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
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the API-level behaviour of the timeline endpoints and the PATCH
 * /reports/{id} test-date endpoint. Trend-calculation correctness is
 * already fully covered by TrendCalculatorTest (Task 2) - these tests
 * focus on HTTP/ownership/shape concerns.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TimelineIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static final byte[] HEMOGLOBIN_PDF = buildPdf("Hemoglobin 13.8 g/dL 13.0 - 17.0");
    private static final byte[] WBC_PDF        = buildPdf("WBC Count 7200 /uL 4000 - 11000");

    // ---- helpers --------------------------------------------------------

    private String loginAs(String email) throws Exception {
        mockMvc.perform(post("/auth/register").contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "firstName", "Anas", "lastName", "Shaikh",
                        "email", email, "password", "password123"))));
        String resp = mockMvc.perform(post("/auth/login").contentType("application/json")
                .content(objectMapper.writeValueAsString(
                        Map.of("email", email, "password", "password123"))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("accessToken").asText();
    }

    private Long uploadAndProcess(String token, byte[] pdfBytes) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", pdfBytes);
        String uploadResp = mockMvc.perform(multipart("/reports").file(file)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        Long reportId = objectMapper.readTree(uploadResp).get("id").asLong();
        mockMvc.perform(post("/reports/" + reportId + "/process")
                .header("Authorization", "Bearer " + token));
        return reportId;
    }

    private static byte[] buildPdf(String text) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---- tests ----------------------------------------------------------

    @Test
    void getAllTrends_emptyWhenNoReports() throws Exception {
        String token = loginAs("trends-empty@example.com");

        mockMvc.perform(get("/results/trends").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trends").isArray())
                .andExpect(jsonPath("$.trends.length()").value(0));
    }

    @Test
    void getAllTrends_requiresAuth() throws Exception {
        mockMvc.perform(get("/results/trends"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTrendForTest_404WhenNoResultsForThatTest() throws Exception {
        String token = loginAs("trends-404@example.com");

        mockMvc.perform(get("/results/trends/hemoglobin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllTrends_afterProcessingReport_returnsExtractedTest() throws Exception {
        String token = loginAs("trends-single@example.com");
        uploadAndProcess(token, HEMOGLOBIN_PDF);

        mockMvc.perform(get("/results/trends").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trends[0].canonicalName").value("hemoglobin"))
                .andExpect(jsonPath("$.trends[0].displayName").value("Hemoglobin"))
                .andExpect(jsonPath("$.trends[0].category").value("CBC"))
                .andExpect(jsonPath("$.trends[0].observations[0].value").value(13.8))
                // One observation → INSUFFICIENT_DATA (minimum 3 required, 6.7)
                .andExpect(jsonPath("$.trends[0].trend").value("INSUFFICIENT_DATA"));
    }

    @Test
    void getTrendForTest_byCanonicalName_returnsCorrectShape() throws Exception {
        String token = loginAs("trends-byname@example.com");
        uploadAndProcess(token, HEMOGLOBIN_PDF);

        mockMvc.perform(get("/results/trends/hemoglobin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canonicalName").value("hemoglobin"))
                .andExpect(jsonPath("$.observations[0].reportId").isNumber())
                .andExpect(jsonPath("$.observations[0].reportResultId").isNumber())
                // Source traceability fields must always be present (6.16)
                .andExpect(jsonPath("$.observations[0].referenceLow").value(13.0))
                .andExpect(jsonPath("$.observations[0].referenceHigh").value(17.0))
                .andExpect(jsonPath("$.observations[0].status").value("NORMAL"));
    }

    @Test
    void userBCannotSeeUserAsTrends() throws Exception {
        String tokenA = loginAs("trends-owner@example.com");
        String tokenB = loginAs("trends-other@example.com");
        uploadAndProcess(tokenA, HEMOGLOBIN_PDF);

        // B's trends list must be empty - A's hemoglobin never appears
        mockMvc.perform(get("/results/trends").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trends.length()").value(0));

        // B's direct lookup for hemoglobin must 404 (not expose A's data)
        mockMvc.perform(get("/results/trends/hemoglobin")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void multipleDifferentTests_eachAppearsAsSeparateTrend() throws Exception {
        String token = loginAs("trends-multi@example.com");
        uploadAndProcess(token, HEMOGLOBIN_PDF);
        uploadAndProcess(token, WBC_PDF);

        mockMvc.perform(get("/results/trends").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trends.length()").value(2));
    }

    @Test
    void patchTestDate_setsDateAndReturnsUpdatedReport() throws Exception {
        String token = loginAs("testdate-set@example.com");
        Long reportId = uploadAndProcess(token, HEMOGLOBIN_PDF);

        mockMvc.perform(patch("/reports/" + reportId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"testDate\":\"2026-01-10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testDate").value("2026-01-10"));
    }

    @Test
    void patchTestDate_acceptsNullToClearDate() throws Exception {
        String token = loginAs("testdate-clear@example.com");
        Long reportId = uploadAndProcess(token, HEMOGLOBIN_PDF);

        // Set it first
        mockMvc.perform(patch("/reports/" + reportId)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"testDate\":\"2026-01-10\"}"));

        // Then clear it
        mockMvc.perform(patch("/reports/" + reportId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"testDate\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testDate").doesNotExist());
    }

    @Test
    void patchTestDate_userBCannotPatchUserAsReport() throws Exception {
        String tokenA = loginAs("testdate-owner@example.com");
        String tokenB = loginAs("testdate-other@example.com");
        Long reportId = uploadAndProcess(tokenA, HEMOGLOBIN_PDF);

        mockMvc.perform(patch("/reports/" + reportId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType("application/json")
                        .content("{\"testDate\":\"2026-01-10\"}"))
                .andExpect(status().isNotFound());
    }
}