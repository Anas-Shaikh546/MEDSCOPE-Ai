package com.medscope.intelligence.client;

import com.medscope.intelligence.entity.InsightPriority;
import com.medscope.intelligence.entity.InsightType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One insight returned from the AI, before validation and persistence.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedInsightDto {

    private InsightType type;
    private String title;
    private String description;
    private InsightPriority priority;
    private Double confidence;
    private List<Long> sourceResultIds;
    private String followUpQuestions;
}
