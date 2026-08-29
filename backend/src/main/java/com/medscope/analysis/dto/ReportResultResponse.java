package com.medscope.analysis.dto;

import com.medscope.analysis.entity.ReportResult;
import com.medscope.analysis.entity.ResultStatus;

/**
 * Deliberately excludes rawValue and confidence from what the frontend
 * sees for now - both exist in the entity/DB for traceability (4.8,
 * 4.13) but aren't part of Step 4's UI requirements. Easy to add later
 * without a schema change.
 */
public record ReportResultResponse(
        String testName,
        Double value,
        String textValue,
        String unit,
        Double referenceLow,
        Double referenceHigh,
        ResultStatus status
) {
    public static ReportResultResponse from(ReportResult result) {
        return new ReportResultResponse(
                result.getTestName(),
                result.getNumericValue(),
                result.getTextValue(),
                result.getUnit(),
                result.getReferenceLow(),
                result.getReferenceHigh(),
                result.getStatus()
        );
    }
}