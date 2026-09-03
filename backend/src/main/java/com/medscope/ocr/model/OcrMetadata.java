package com.medscope.ocr.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Document-level OCR metadata stored with the report.
 * Tracks whether OCR was used and with what confidence.
 */
@Value
@Builder
public class OcrMetadata {
    /**
     * Whether OCR was used for this report
     */
    boolean ocrUsed;

    /**
     * OCR engine name (e.g., "PaddleOCR", "Tesseract")
     */
    String ocrEngine;

    /**
     * OCR engine version
     */
    String ocrEngineVersion;

    /**
     * List of page numbers that required OCR (zero-based)
     */
    List<Integer> ocrPages;

    /**
     * Overall OCR confidence across all pages (0.0 - 1.0)
     */
    double averageConfidence;

    /**
     * When OCR processing was performed
     */
    java.time.Instant processedAt;

    /**
     * Number of OCR attempts/retries
     */
    int attempts;

    /**
     * Whether preprocessing was applied
     */
    boolean preprocessingApplied;
}
