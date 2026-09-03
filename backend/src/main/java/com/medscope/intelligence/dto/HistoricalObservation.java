package com.medscope.intelligence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One historical data point for a test, with its date and reference
 * range context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalObservation {

    private String date;
    private boolean dateIsConfirmed;
    private Long reportResultId;
    private Double value;
    private String unit;
    private Double referenceLow;
    private Double referenceHigh;
    private String status;
}
