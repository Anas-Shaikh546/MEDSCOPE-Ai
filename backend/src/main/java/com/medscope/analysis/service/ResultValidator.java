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

    /**
     * Handles three cases explicitly, never falling through to a
     * default: a full two-sided range, a one-sided threshold ("< 200"
     * or "> 40"), or no range at all. Only the last is UNKNOWN - a
     * partial range still tells us something real about the value.
     */
    private ResultStatus computeStatus(ExtractedResult extracted) {
        Double value = extracted.numericValue();
        Double low = extracted.referenceLow();
        Double high = extracted.referenceHigh();

        // Qualitative results have no numeric value at all.
        if (value == null) {
            return ResultStatus.UNKNOWN;
        }

        if (low != null && high != null) {
            if (value < low) {
                return ResultStatus.LOW;
            }
            if (value > high) {
                return ResultStatus.HIGH;
            }
            return ResultStatus.NORMAL;
        }

        // Only an upper threshold is known (e.g. "< 200") - exceeding
        // it is HIGH, there is no LOW without a lower bound.
        if (high != null) {
            return value > high ? ResultStatus.HIGH : ResultStatus.NORMAL;
        }

        // Only a lower threshold is known (e.g. "> 40") - falling
        // short of it is LOW, there is no HIGH without an upper bound.
        if (low != null) {
            return value < low ? ResultStatus.LOW : ResultStatus.NORMAL;
        }

        // No reference information in the report at all - never
        // guessed (4.11, 4.12).
        return ResultStatus.UNKNOWN;
    }
}