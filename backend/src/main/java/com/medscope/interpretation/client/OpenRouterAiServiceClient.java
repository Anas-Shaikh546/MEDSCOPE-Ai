package com.medscope.interpretation.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medscope.interpretation.dto.AnalyzeRequest;
import com.medscope.interpretation.dto.AnalyzeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * OpenRouter-backed implementation of AiServiceClient. Calls the FastAPI
 * ai-service which in turn calls OpenRouter.
 *
 * HTTP/1.1 is forced explicitly: Java's HttpClient defaults to attempting
 * an HTTP/2 cleartext upgrade even for plain http:// URLs, which uvicorn
 * doesn't support - it silently mangled the request body, causing FastAPI
 * to see an empty body on every call. Fixed by pinning HTTP_1_1 here.
 *
 * To switch providers: implement AiServiceClient with a different
 * @Component and change the @Primary annotation (or use @Qualifier at
 * the injection site in AnalysisService). This class doesn't need to
 * change.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenRouterAiServiceClient implements AiServiceClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Override
    public AnalyzeResponse analyze(AnalyzeRequest request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/analyze"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            log.info("Calling AI service: reportId={}", request.getReportId());

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                log.error("AI service returned error: status={}, body={}",
                        response.statusCode(), response.body());
                throw new AiServiceException(
                        "AI service returned status " + response.statusCode()
                );
            }

            AnalyzeResponse analyzeResponse = objectMapper.readValue(
                    response.body(),
                    AnalyzeResponse.class
            );

            validateResponse(analyzeResponse);

            log.info("AI analysis completed: reportId={}, status={}, findings={}",
                    request.getReportId(),
                    analyzeResponse.getStatus(),
                    analyzeResponse.getFindings().size()
            );

            return analyzeResponse;

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call AI service: reportId={}", request.getReportId(), e);
            throw new AiServiceException("Failed to communicate with AI service", e);
        }
    }

    private void validateResponse(AnalyzeResponse response) {
        if (response.getStatus() == null) {
            throw new AiServiceException("AI service response missing status");
        }

        if ("FAILED".equals(response.getStatus())) {
            throw new AiServiceException(
                    "AI analysis failed: " + response.getSummary()
            );
        }

        if (isBlank(response.getModelName()) || isBlank(response.getModelVersion())
                || isBlank(response.getPromptVersion())) {
            throw new AiServiceException(
                    "AI service response missing model/prompt version metadata"
            );
        }

        if (response.getFindings() == null) {
            throw new AiServiceException("AI service response missing findings");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}