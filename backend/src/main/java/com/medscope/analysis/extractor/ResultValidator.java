package com.medscope.analysis.service;

import com.medscope.analysis.entity.ReportResult;
import com.medscope.analysis.entity.ResultStatus;
import com.medscope.analysis.extractor.ExtractedResult;
import org.springframework.stereotype.Component;

/**
 * Last stop before persistence: computes each result's status relative
 * to its own reference range (never a diagnosis - 4.12) and converts
 * the intermediate ExtractedResult into a persistable ReportResult.
 *
 * A result with a range is never left UNKNOWN, and a result without one
 * never gets a guessed range - the two are handled explicitly rather
 * than falling through to a default.
 */
@Component
public class ResultValidator {

    public ReportResult toReportResult(Long reportId, ExtractedResult extracted) {
        ResultStatus status = computeStatus(extracted);

        return ReportResult.builder()
                .reportId(reportId)
                .testName(extracted.testName())
                .normalizedTestName(extracted.normalizedTestName())
                .rawValue(extracted.rawValue())
                .numericValue(extracted.numericValue())
                .textValue(extracted.textValue())
                .unit(extracted.unit())
                .referenceLow(extracted.referenceLow())
                .referenceHigh(extracted.referenceHigh())
                .status(status)
                .confidence(extracted.confidence())
                .build();
    }

    private ResultStatus computeStatus(ExtractedResult extracted) {
        Double value = extracted.numericValue();
        Double low = extracted.referenceLow();
        Double high = extracted.referenceHigh();

        // Qualitative results, or numeric results with no reference
        // range in the report, are UNKNOWN - never guessed (4.11, 4.12).
        if (value == null || low == null || high == null) {
            return ResultStatus.UNKNOWN;
        }

        if (value < low) {
            return ResultStatus.LOW;
        }
        if (value > high) {
            return ResultStatus.HIGH;
        }
        return ResultStatus.NORMAL;
    }
}