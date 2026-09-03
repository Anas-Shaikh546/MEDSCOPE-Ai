package com.medscope.ocr.service;

import lombok.Builder;
import lombok.Value;

/**
 * Individual word-level OCR result with position and confidence.
 * Used for layout-aware extraction and confidence tracking.
 */
@Value
@Builder
public class OcrWord {
    /**
     * The recognized word text
     */
    String text;

    /**
     * Confidence score for this word (0.0 - 1.0)
     */
    double confidence;

    /**
     * Bounding box coordinates (x, y, width, height)
     */
    BoundingBox boundingBox;

    /**
     * Line number within the page
     */
    int lineNumber;

    /**
     * Block/region number (for multi-column layouts)
     */
    int blockNumber;
}
