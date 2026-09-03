package com.medscope.intelligence.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.medscope.intelligence.dto.IntelligenceContext;
import com.medscope.intelligence.entity.InsightPriority;
import com.medscope.intelligence.entity.InsightType;
import com.medscope.intelligence.service.PrioritizationFlag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for the FastAPI intelligence service. Mirrors the pattern
 * from OpenRouterAiServiceClient (Step 5) but calls the new
 * /intelligence/generate endpoint.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FastApiIntelligenceClient implements IntelligenceAiClient {

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    private final ObjectMapper objectMapper;

    private static final Duration TIMEOUT = Duration.ofSeconds(120);

    @Override
    public IntelligenceResponse generateInsights(
            IntelligenceContext context,
            List<PrioritizationFlag> flags
    ) {
        try {
            String endpoint = aiServiceUrl + "/intelligence/generate";
            log.debug("Calling intelligence endpoint: {}", endpoint);

            // Create ObjectMapper with snake_case for FastAPI
            ObjectMapper snakeCaseMapper = new ObjectMapper();
            snakeCaseMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("context", context);
            requestBody.put("flags", flags);

            String requestJson = snakeCaseMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .version(HttpClient.Version.HTTP_1_1)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Intelligence service returned status {}: {}", response.statusCode(), response.body());
                return IntelligenceResponse.builder()
                        .status("FAILED")
                        .insights(new ArrayList<>())
                        .errorMessage("AI service returned status " + response.statusCode())
                        .build();
            }

            return parseResponse(response.body());

        } catch (Exception e) {
            log.error("Intelligence generation failed", e);
            return IntelligenceResponse.builder()
                    .status("FAILED")
                    .insights(new ArrayList<>())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private IntelligenceResponse parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        String status = root.path("status").asText();
        String modelName = root.path("model_name").asText();
        String modelVersion = root.path("model_version").asText();
        String promptVersion = root.path("prompt_version").asText();
        String errorMessage = root.path("error_message").asText(null);

        List<GeneratedInsightDto> insights = new ArrayList<>();
        JsonNode insightsArray = root.path("insights");
        if (insightsArray.isArray()) {
            for (JsonNode insightNode : insightsArray) {
                insights.add(GeneratedInsightDto.builder()
                        .type(InsightType.valueOf(insightNode.path("type").asText()))
                        .title(insightNode.path("title").asText())
                        .description(insightNode.path("description").asText())
                        .priority(InsightPriority.valueOf(insightNode.path("priority").asText()))
                        .confidence(insightNode.path("confidence").isNull() ? null : insightNode.path("confidence").asDouble())
                        .sourceResultIds(parseSourceIds(insightNode.path("source_result_ids")))
                        .followUpQuestions(insightNode.path("follow_up_questions").asText(null))
                        .build());
            }
        }

        return IntelligenceResponse.builder()
                .status(status)
                .insights(insights)
                .modelName(modelName)
                .modelVersion(modelVersion)
                .promptVersion(promptVersion)
                .errorMessage(errorMessage)
                .build();
    }

    private List<Long> parseSourceIds(JsonNode arrayNode) {
        List<Long> ids = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode idNode : arrayNode) {
                ids.add(idNode.asLong());
            }
        }
        return ids;
    }
}
