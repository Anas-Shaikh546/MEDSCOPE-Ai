package com.medscope.analysis.extractor;

/**
 * One candidate result parsed from a line of report text, before
 * validation (Task 3's MedicalResultExtractor output -> ResultValidator
 * input). Exactly one of numericValue/textValue is populated - see
 * spec 4.9, qualitative results (e.g. "Non-reactive") are never forced
 * into a number.
 */
public record ExtractedResult(
        String testName,
        String normalizedTestName,
        String rawValue,
        Double numericValue,
        String textValue,
        String unit,
        Double referenceLow,
        Double referenceHigh,
        double confidence
) {
}