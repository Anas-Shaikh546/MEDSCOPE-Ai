package com.medscope.intelligence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Historical observations for one test, with trend direction from
 * Step 6's TrendCalculator. The trend direction is deterministic -
 * the AI's job is to explain what the trend means in context, not
 * to recalculate it (step7.txt section 3: "Step 6: 'Trend = INCREASING',
 * Step 7 AI: 'Explain what this observed trend means in context'").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestTrendContext {

    private String canonicalName;
    private String displayName;
    private String trendDirection;
    private List<HistoricalObservation> observations;
}
