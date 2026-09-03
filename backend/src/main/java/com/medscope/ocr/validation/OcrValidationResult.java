package com.medscope.ocr.validation;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Validation result for OCR-extracted medical data.
 * Philosophy: Never trust OCR blindly - validate before accepting as medical data.
 */
@Value
@Builder
public class OcrValidationResult {
    boolean valid;
    OcrConfidenceLevel confidenceLevel;
    List<String> warnings;
    List<String> errors;

    /**
     * Original OCR text that failed validation (preserved for debugging)
     */
    String rejectedText;
}
