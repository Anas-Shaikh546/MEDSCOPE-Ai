package com.medscope.intelligence.client;

import com.medscope.intelligence.dto.IntelligenceContext;
import com.medscope.intelligence.service.PrioritizationFlag;

import java.util.List;

/**
 * Abstraction for AI intelligence generation. Follows the same provider
 * abstraction pattern as Step 5's AiServiceClient (see step7.txt section 10:
 * "Do not hardwire Step 7 to a provider").
 */
public interface IntelligenceAiClient {

    /**
     * Generate structured insights from intelligence context and deterministic flags.
     *
     * @param context assembled context from ReportResult + Analysis + Timeline
     * @param flags deterministic flags from PrioritizationEngine
     * @return AI-generated insights with provenance
     */
    IntelligenceResponse generateInsights(
            IntelligenceContext context,
            List<PrioritizationFlag> flags
    );
}
