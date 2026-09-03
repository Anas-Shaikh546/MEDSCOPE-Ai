package com.medscope.intelligence.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from intelligence AI provider, before validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntelligenceResponse {

    private String status;
    private List<GeneratedInsightDto> insights;
    private String modelName;
    private String modelVersion;
    private String promptVersion;
    private String errorMessage;
}
