package com.medscope.intelligence.service;

import com.medscope.intelligence.dto.InsightDto;
import com.medscope.intelligence.entity.InsightGenerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Summary of one insight generation for API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightGenerationSummary {

    private Long generationId;
    private Long reportId;
    private Integer generationNumber;
    private InsightGenerationStatus status;
    private Integer insightCount;
    private String createdAt; // ISO-8601 string
    private List<InsightDto> insights;
}
