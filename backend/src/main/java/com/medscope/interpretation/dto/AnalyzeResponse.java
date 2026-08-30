package com.medscope.interpretation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("recommendations")
    private String recommendations;

    @JsonProperty("findings")
    private List<AnalysisFindingDto> findings;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("prompt_version")
    private String promptVersion;
}
