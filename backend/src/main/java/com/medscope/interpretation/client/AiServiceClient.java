package com.medscope.interpretation.client;

import com.medscope.interpretation.dto.AnalyzeRequest;
import com.medscope.interpretation.dto.AnalyzeResponse;

/**
 * Contract for the AI interpretation service. The concrete implementation
 * is currently OpenRouterAiServiceClient (OpenRouter/Nemotron). Swapping
 * to a different provider (Anthropic, Gemini, OpenAI, a local model) means
 * adding a new implementation of this interface and changing which bean
 * Spring injects - AnalysisService never needs to change (Measure 10).
 */
public interface AiServiceClient {

    /**
     * Send structured report facts to the AI service and receive a
     * validated, structured interpretation back.
     *
     * @throws AiServiceException if the call fails or the response is invalid
     */
    AnalyzeResponse analyze(AnalyzeRequest request);
}