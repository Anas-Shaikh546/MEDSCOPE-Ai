package com.medscope.ocr.validation;

import com.medscope.ocr.service.OcrResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates OCR output before accepting as medical data.
 * CRITICAL: Reject uncertain values - never guess "18.8" from "18.B"
 */
@Component
@Slf4j
public class MedicalOcrValidator {

    private static final double HIGH_CONFIDENCE = 0.90;
    private static final double MEDIUM_CONFIDENCE = 0.75;

    // Suspicious patterns that indicate OCR errors
    private static final Pattern MIXED_ALPHANUMERIC = Pattern.compile("\\d+[A-Z]+\\d*|\\d*[A-Z]+\\d+");
    private static final Pattern INVALID_DECIMAL = Pattern.compile("\\d+\\.[A-Z]|\\d+[A-Z]\\.");

    public OcrValidationResult validate(OcrResult ocrResult) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Check 1: Overall confidence
        OcrConfidenceLevel level = determineConfidenceLevel(ocrResult.getConfidence());
        if (level == OcrConfidenceLevel.LOW) {
            errors.add("Overall OCR confidence too low: " + ocrResult.getConfidence());
            return buildRejected(level, errors, warnings, ocrResult.getRawText());
        }

        // Check 2: Suspicious patterns
        String text = ocrResult.getRawText();
        if (MIXED_ALPHANUMERIC.matcher(text).find()) {
            warnings.add("Found mixed alphanumeric patterns - possible OCR error");
        }
        if (INVALID_DECIMAL.matcher(text).find()) {
            errors.add("Found invalid decimal patterns (e.g., 18.B) - rejecting");
            return buildRejected(level, errors, warnings, text);
        }

        // Check 3: Word-level confidence
        long lowConfidenceWords = ocrResult.getWords().stream()
            .filter(w -> w.getConfidence() < MEDIUM_CONFIDENCE)
            .count();

        if (lowConfidenceWords > ocrResult.getWords().size() * 0.3) {
            errors.add("Too many low-confidence words: " + lowConfidenceWords);
            return buildRejected(level, errors, warnings, text);
        }

        log.info("OCR validation passed: confidence={}, warnings={}", level, warnings.size());
        return OcrValidationResult.builder()
            .valid(true)
            .confidenceLevel(level)
            .warnings(warnings)
            .errors(List.of())
            .build();
    }

    private OcrConfidenceLevel determineConfidenceLevel(double confidence) {
        if (confidence >= HIGH_CONFIDENCE) return OcrConfidenceLevel.HIGH;
        if (confidence >= MEDIUM_CONFIDENCE) return OcrConfidenceLevel.MEDIUM;
        return OcrConfidenceLevel.LOW;
    }

    private OcrValidationResult buildRejected(OcrConfidenceLevel level,
                                              List<String> errors,
                                              List<String> warnings,
                                              String text) {
        log.warn("OCR validation FAILED: errors={}", errors);
        return OcrValidationResult.builder()
            .valid(false)
            .confidenceLevel(level)
            .warnings(warnings)
            .errors(errors)
            .rejectedText(text)
            .build();
    }
}
