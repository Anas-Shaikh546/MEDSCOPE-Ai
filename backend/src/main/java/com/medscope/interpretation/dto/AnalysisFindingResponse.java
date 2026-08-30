package com.medscope.interpretation.dto;

import com.medscope.interpretation.entity.AnalysisFinding;
import com.medscope.interpretation.entity.AnalysisSeverity;

public record AnalysisFindingResponse(
        Long reportResultId,
        String interpretation,
        AnalysisSeverity severity
) {
    public static AnalysisFindingResponse from(AnalysisFinding finding) {
        return new AnalysisFindingResponse(
                finding.getReportResultId(),
                finding.getInterpretation(),
                finding.getSeverity()
        );
    }
}
