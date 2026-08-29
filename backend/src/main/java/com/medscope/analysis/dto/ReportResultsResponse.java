package com.medscope.analysis.dto;

import com.medscope.report.entity.ReportStatus;

import java.util.List;

public record ReportResultsResponse(
        Long reportId,
        ReportStatus status,
        List<ReportResultResponse> results
) {
}