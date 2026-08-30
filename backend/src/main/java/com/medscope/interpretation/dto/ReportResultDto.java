package com.medscope.interpretation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResultDto {

    @JsonProperty("test_name")
    private String testName;

    @JsonProperty("normalized_test_name")
    private String normalizedTestName;

    @JsonProperty("raw_value")
    private String rawValue;

    @JsonProperty("numeric_value")
    private Double numericValue;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("reference_low")
    private Double referenceLow;

    @JsonProperty("reference_high")
    private Double referenceHigh;

    @JsonProperty("status")
    private String status;

    @JsonProperty("confidence")
    private Double confidence;

    @JsonProperty("page_number")
    private Integer pageNumber;
}
