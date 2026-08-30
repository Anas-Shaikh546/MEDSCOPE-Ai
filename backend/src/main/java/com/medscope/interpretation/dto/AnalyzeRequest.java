package com.medscope.interpretation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyzeRequest {

    @JsonProperty("report_id")
    private Long reportId;

    @JsonProperty("results")
    private List<ReportResultDto> results;
}
