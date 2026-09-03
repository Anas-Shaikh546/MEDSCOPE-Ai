package com.medscope.ocr.model;

import lombok.Builder;
import lombok.Value;

/**
 * Analysis result for a single PDF page, containing multiple signals
 * used to determine if OCR is required.
 */
@Value
@Builder
public class PageAnalysis {
    /**
     * Zero-based page index
     */
    int pageIndex;

    /**
     * Classification result
     */
    PageType pageType;

    /**
     * Number of characters extracted from text layer
     */
    int textLength;

    /**
     * Text density: characters per square inch (approximate)
     */
    double textDensity;

    /**
     * Whether page contains large images (potential scan)
     */
    boolean hasLargeImages;

    /**
     * Percentage of page area covered by images (0.0 - 1.0)
     */
    double imageAreaRatio;

    /**
     * Whether extracted text appears to be garbled/corrupted
     */
    boolean textIsGarbled;

    /**
     * Overall confidence in this classification (0.0 - 1.0)
     */
    double confidence;
}
