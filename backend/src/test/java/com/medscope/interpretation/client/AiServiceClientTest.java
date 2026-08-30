package com.medscope.interpretation.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medscope.interpretation.dto.AnalysisFindingDto;
import com.medscope.interpretation.dto.AnalyzeRequest;
import com.medscope.interpretation.dto.AnalyzeResponse;
import com.medscope.interpretation.dto.ReportResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AiServiceClient - focuses on JSON serialization/deserialization
 * without requiring full Spring context or database.
 */
class AiServiceClientTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testAnalyzeRequest_serialization() throws Exception {
        ReportResultDto resultDto = ReportResultDto.builder()
                .testName("Hemoglobin")
                .normalizedTestName("hemoglobin")
                .rawValue("12.5")
                .numericValue(12.5)
                .unit("g/dL")
                .referenceLow(12.0)
                .referenceHigh(16.0)
                .status("NORMAL")
                .confidence(0.95)
                .pageNumber(1)
                .build();

        AnalyzeRequest request = AnalyzeRequest.builder()
                .reportId(1L)
                .results(List.of(resultDto))
                .build();

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"report_id\":1"));
        assertTrue(json.contains("\"test_name\":\"Hemoglobin\""));
        assertTrue(json.contains("\"normalized_test_name\":\"hemoglobin\""));
        assertTrue(json.contains("\"numeric_value\":12.5"));
        assertTrue(json.contains("\"status\":\"NORMAL\""));
        assertTrue(json.contains("\"confidence\":0.95"));
    }

    @Test
    void testAnalyzeResponse_deserialization() throws Exception {
        String json = """
                {
                    "status": "COMPLETED",
                    "summary": "Test summary",
                    "recommendations": "Test recommendations",
                    "findings": [
                        {
                            "report_result_index": 0,
                            "interpretation": "Normal hemoglobin levels",
                            "severity": "NORMAL"
                        }
                    ],
                    "model_name": "nvidia/nemotron-3-ultra-550b-a55b:free",
                    "model_version": "1.0",
                    "prompt_version": "1.0"
                }
                """;

        AnalyzeResponse response = objectMapper.readValue(json, AnalyzeResponse.class);

        assertEquals("COMPLETED", response.getStatus());
        assertEquals("Test summary", response.getSummary());
        assertEquals("Test recommendations", response.getRecommendations());
        assertEquals(1, response.getFindings().size());
        assertEquals("nvidia/nemotron-3-ultra-550b-a55b:free", response.getModelName());
        assertEquals("1.0", response.getModelVersion());
        assertEquals("1.0", response.getPromptVersion());

        AnalysisFindingDto finding = response.getFindings().get(0);
        assertEquals(0, finding.getReportResultIndex());
        assertEquals("Normal hemoglobin levels", finding.getInterpretation());
        assertEquals("NORMAL", finding.getSeverity());
    }

    @Test
    void testAnalyzeResponse_deserializationWithMultipleFindings() throws Exception {
        String json = """
                {
                    "status": "COMPLETED",
                    "summary": "Multiple abnormalities detected",
                    "recommendations": "Follow up with physician",
                    "findings": [
                        {
                            "report_result_index": 0,
                            "interpretation": "Hemoglobin below normal range",
                            "severity": "CONCERN"
                        },
                        {
                            "report_result_index": 1,
                            "interpretation": "Blood pressure elevated",
                            "severity": "ATTENTION"
                        }
                    ],
                    "model_name": "nvidia/nemotron-3-ultra-550b-a55b:free",
                    "model_version": "1.0",
                    "prompt_version": "1.0"
                }
                """;

        AnalyzeResponse response = objectMapper.readValue(json, AnalyzeResponse.class);

        assertEquals("COMPLETED", response.getStatus());
        assertEquals(2, response.getFindings().size());
        assertEquals("CONCERN", response.getFindings().get(0).getSeverity());
        assertEquals("ATTENTION", response.getFindings().get(1).getSeverity());
    }

    @Test
    void testAnalyzeResponse_deserializationWithFailedStatus() throws Exception {
        String json = """
                {
                    "status": "FAILED",
                    "summary": "Analysis failed due to invalid input",
                    "recommendations": null,
                    "findings": [],
                    "model_name": "nvidia/nemotron-3-ultra-550b-a55b:free",
                    "model_version": "1.0",
                    "prompt_version": "1.0"
                }
                """;

        AnalyzeResponse response = objectMapper.readValue(json, AnalyzeResponse.class);

        assertEquals("FAILED", response.getStatus());
        assertEquals("Analysis failed due to invalid input", response.getSummary());
        assertNull(response.getRecommendations());
        assertEquals(0, response.getFindings().size());
    }

    @Test
    void testAnalyzeResponse_preservesModelAndPromptMetadataForPersistence() throws Exception {
        String json = """
                {
                    "status": "COMPLETED",
                    "summary": "Test summary",
                    "recommendations": null,
                    "findings": [],
                    "model_name": "provider/model-id",
                    "model_version": "model-release-2",
                    "prompt_version": "v2.0"
                }
                """;

        AnalyzeResponse response = objectMapper.readValue(json, AnalyzeResponse.class);

        assertEquals("provider/model-id", response.getModelName());
        assertEquals("model-release-2", response.getModelVersion());
        assertEquals("v2.0", response.getPromptVersion());
    }

    @Test
    void testReportResultDto_allFields() throws Exception {
        ReportResultDto dto = ReportResultDto.builder()
                .testName("Glucose")
                .normalizedTestName("glucose")
                .rawValue("110")
                .numericValue(110.0)
                .unit("mg/dL")
                .referenceLow(70.0)
                .referenceHigh(100.0)
                .status("ABNORMAL")
                .confidence(0.98)
                .pageNumber(2)
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"test_name\":\"Glucose\""));
        assertTrue(json.contains("\"raw_value\":\"110\""));
        assertTrue(json.contains("\"reference_low\":70.0"));
        assertTrue(json.contains("\"reference_high\":100.0"));
        assertTrue(json.contains("\"page_number\":2"));
    }

    @Test
    void testAiServiceException_message() {
        AiServiceException exception = new AiServiceException("Test error message");
        assertEquals("Test error message", exception.getMessage());
    }

    @Test
    void testAiServiceException_withCause() {
        Exception cause = new RuntimeException("Root cause");
        AiServiceException exception = new AiServiceException("Test error", cause);

        assertEquals("Test error", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
