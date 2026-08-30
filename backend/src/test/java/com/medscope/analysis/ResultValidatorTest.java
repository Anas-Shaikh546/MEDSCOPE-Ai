package com.medscope.analysis;

import com.medscope.analysis.entity.ReportResult;
import com.medscope.analysis.entity.ResultStatus;
import com.medscope.analysis.extractor.ExtractedResult;
import com.medscope.analysis.service.ResultValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultValidatorTest {

    private final ResultValidator validator = new ResultValidator();

    @Test
    void valueWithinRange_isNormal() {
        ExtractedResult extracted = new ExtractedResult(
                "Hemoglobin", "hemoglobin", "13.8", 13.8, null, "g/dL", 13.0, 17.0, 0.95);

        ReportResult result = validator.toReportResult(42L, extracted);

        assertEquals(ResultStatus.NORMAL, result.getStatus());
        assertEquals(42L, result.getReportId());
    }

    @Test
    void valueBelowRange_isLow() {
        ExtractedResult extracted = new ExtractedResult(
                "Hemoglobin", "hemoglobin", "11.0", 11.0, null, "g/dL", 13.0, 17.0, 0.95);

        ReportResult result = validator.toReportResult(1L, extracted);

        assertEquals(ResultStatus.LOW, result.getStatus());
    }

    @Test
    void valueAboveRange_isHigh() {
        ExtractedResult extracted = new ExtractedResult(
                "Hemoglobin", "hemoglobin", "18.0", 18.0, null, "g/dL", 13.0, 17.0, 0.95);

        ReportResult result = validator.toReportResult(1L, extracted);

        assertEquals(ResultStatus.HIGH, result.getStatus());
    }

    @Test
    void missingReferenceRange_isUnknown_notGuessed() {
        ExtractedResult extracted = new ExtractedResult(
                "Glucose", "glucose", "95", 95.0, null, "mg/dL", null, null, 0.75);

        ReportResult result = validator.toReportResult(1L, extracted);

        assertEquals(ResultStatus.UNKNOWN, result.getStatus());
        assertNull(result.getReferenceLow());
        assertNull(result.getReferenceHigh());
    }

    @Test
    void qualitativeResult_isUnknown() {
        ExtractedResult extracted = new ExtractedResult(
                "HIV", null, "Non-reactive", null, "Non-reactive", null, null, null, 0.85);

        ReportResult result = validator.toReportResult(1L, extracted);

        assertEquals(ResultStatus.UNKNOWN, result.getStatus());
        assertEquals("Non-reactive", result.getTextValue());
        assertNull(result.getNumericValue());
    }

    @Test
    void rawValueIsAlwaysPreserved() {
        ExtractedResult extracted = new ExtractedResult(
                "Hemoglobin", "hemoglobin", "13.8", 13.8, null, "g/dL", 13.0, 17.0, 0.95);

        ReportResult result = validator.toReportResult(1L, extracted);

        assertEquals("13.8", result.getRawValue());
    }

    @Test
    void upperThresholdOnly_valueBelowThreshold_isNormal() {
        // "Total Cholesterol 185 mg/dL < 200" - 185 is desirable
        ExtractedResult extracted = new ExtractedResult(
                "Total Cholesterol", "total_cholesterol", "185", 185.0, null, "mg/dL", null, 200.0, 0.90);

        ReportResult result = validator.toReportResult(1L, extracted);

        assertEquals(ResultStatus.NORMAL, result.getStatus());
    }

    @Test
    void upperThresholdOnly_valueAboveThreshold_isHigh() {
        ExtractedResult extracted = new ExtractedResult(
                "Total Cholesterol", "total_cholesterol", "220", 220.0, null, "mg/dL", null, 200.0, 0.90);

        ReportResult result = validator.toReportResult(1L, extracted);

        assertEquals(ResultStatus.HIGH, result.getStatus());
    }

    @Test
    void lowerThresholdOnly_valueAboveThreshold_isNormal() {
        // "HDL 55 mg/dL > 40" - 55 is desirable
        ExtractedResult extracted = new ExtractedResult(
                "HDL", "hdl", "55", 55.0, null, "mg/dL", 40.0, null, 0.90);

        ReportResult result = validator.toReportResult(1L, extracted);

        assertEquals(ResultStatus.NORMAL, result.getStatus());
    }

    @Test
    void lowerThresholdOnly_valueBelowThreshold_isLow() {
        ExtractedResult extracted = new ExtractedResult(
                "HDL", "hdl", "30", 30.0, null, "mg/dL", 40.0, null, 0.90);

        ReportResult result = validator.toReportResult(1L, extracted);

        assertEquals(ResultStatus.LOW, result.getStatus());
    }
}