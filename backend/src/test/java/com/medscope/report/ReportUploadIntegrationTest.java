package com.medscope.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the upload + ownership subset of the Step 3.24 checklist that
 * Task 3 (ReportService + ReportController) is responsible for.
 * Download/delete get their own tests in Task 4.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4 fake but valid header".getBytes();

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

    @Test
    void validPdfUpload_returns201AndAppearsInList() throws Exception {
        String token = registerAndLogin("uploader@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "blood_report.pdf", "application/pdf", VALID_PDF_BYTES);

        mockMvc.perform(multipart("/reports").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("blood_report.pdf"))
                .andExpect(jsonPath("$.status").value("UPLOADED"));

        mockMvc.perform(get("/reports").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalFilename").value("blood_report.pdf"));
    }

    @Test
    void emptyFile_returns400() throws Exception {
        String token = registerAndLogin("empty@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/reports").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonPdfExtension_returns400() throws Exception {
        String token = registerAndLogin("nonpdf@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "report.txt", "text/plain", "just text".getBytes());

        mockMvc.perform(multipart("/reports").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fakePdf_wrongMimeType_returns400() throws Exception {
        String token = registerAndLogin("fakemime@example.com");

        // .pdf extension but declares itself as octet-stream - must fail the MIME check.
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.pdf", "application/octet-stream", VALID_PDF_BYTES);

        mockMvc.perform(multipart("/reports").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fakePdf_wrongSignature_returns400() throws Exception {
        String token = registerAndLogin("fakesig@example.com");

        // correct extension and MIME type, but not actually a PDF (no %PDF- header).
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", "not a real pdf".getBytes());

        mockMvc.perform(multipart("/reports").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userBCannotSeeUserAsReportInList() throws Exception {
        String tokenA = registerAndLogin("ownera@example.com");
        String tokenB = registerAndLogin("ownerb@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "private.pdf", "application/pdf", VALID_PDF_BYTES);
        mockMvc.perform(multipart("/reports").file(file).header("Authorization", "Bearer " + tokenA));

        mockMvc.perform(get("/reports").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void userBCannotGetUserAsReportById_returns404() throws Exception {
        String tokenA = registerAndLogin("targeta@example.com");
        String tokenB = registerAndLogin("targetb@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "private.pdf", "application/pdf", VALID_PDF_BYTES);
        String uploadResponse = mockMvc.perform(multipart("/reports").file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andReturn().getResponse().getContentAsString();
        Long reportId = objectMapper.readTree(uploadResponse).get("id").asLong();

        mockMvc.perform(get("/reports/" + reportId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/reports/" + reportId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    @Test
    void ownerCanDownloadTheirOwnFile() throws Exception {
        String token = registerAndLogin("downloader@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "my_report.pdf", "application/pdf", VALID_PDF_BYTES);
        String uploadResponse = mockMvc.perform(multipart("/reports").file(file)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        Long reportId = objectMapper.readTree(uploadResponse).get("id").asLong();

        mockMvc.perform(get("/reports/" + reportId + "/file").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void userBCannotDownloadUserAsFile_returns404() throws Exception {
        String tokenA = registerAndLogin("dlowner@example.com");
        String tokenB = registerAndLogin("dlother@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "private.pdf", "application/pdf", VALID_PDF_BYTES);
        String uploadResponse = mockMvc.perform(multipart("/reports").file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andReturn().getResponse().getContentAsString();
        Long reportId = objectMapper.readTree(uploadResponse).get("id").asLong();

        mockMvc.perform(get("/reports/" + reportId + "/file").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanDeleteTheirOwnReport_andItDisappearsFromList() throws Exception {
        String token = registerAndLogin("deleter@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "to_delete.pdf", "application/pdf", VALID_PDF_BYTES);
        String uploadResponse = mockMvc.perform(multipart("/reports").file(file)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        Long reportId = objectMapper.readTree(uploadResponse).get("id").asLong();

        mockMvc.perform(delete("/reports/" + reportId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/reports/" + reportId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/reports").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void userBCannotDeleteUserAsReport_returns404AndReportStillExistsForOwner() throws Exception {
        String tokenA = registerAndLogin("delowner@example.com");
        String tokenB = registerAndLogin("delother@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "protected.pdf", "application/pdf", VALID_PDF_BYTES);
        String uploadResponse = mockMvc.perform(multipart("/reports").file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andReturn().getResponse().getContentAsString();
        Long reportId = objectMapper.readTree(uploadResponse).get("id").asLong();

        mockMvc.perform(delete("/reports/" + reportId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/reports/" + reportId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }
}