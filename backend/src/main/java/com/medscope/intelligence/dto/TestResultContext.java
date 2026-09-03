package com.medscope.intelligence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One test result from the current report, flattened for AI consumption.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResultContext {

    private Long resultId;
    private String testName;
    private String canonicalName;
    private Double numericValue;
    private String textValue;
    private String unit;
    private Double referenceLow;
    private Double referenceHigh;
    private String status;
}
