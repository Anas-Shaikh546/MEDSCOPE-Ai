package com.medscope.ocr.service;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Result from OCR processing of a single page.
 * Contains both raw text and confidence/metadata for validation.
 */
@Value
@Builder
public class OcrResult {
    /**
     * Page number (zero-based)
     */
    int pageNumber;

    /**
     * Extracted raw text from OCR
     */
    String rawText;

    /**
     * Overall OCR confidence for this page (0.0 - 1.0)
     */
    double confidence;

    /**
     * Processing time in milliseconds
     */
    long processingTimeMs;

    /**
     * Word-level OCR results with positions and confidence
     */
    List<OcrWord> words;

    /**
     * Whether preprocessing was applied
     */
    boolean preprocessed;

    /**
     * OCR engine used
     */
    String engine;

    /**
     * OCR engine version
     */
    String engineVersion;
}
