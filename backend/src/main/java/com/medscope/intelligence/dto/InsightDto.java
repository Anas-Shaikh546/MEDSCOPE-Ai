package com.medscope.intelligence.dto;

import com.medscope.intelligence.entity.InsightPriority;
import com.medscope.intelligence.entity.InsightStatus;
import com.medscope.intelligence.entity.InsightType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Insight entity for API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightDto {

    private Long id;
    private Long generationId;
    private InsightType type;
    private String title;
    private String description;
    private InsightPriority priority;
    private String followUpQuestions;
    private Double confidence;
    private InsightStatus status;
    private String createdAt; // ISO-8601 string
}
