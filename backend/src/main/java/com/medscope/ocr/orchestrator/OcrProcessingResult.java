package com.medscope.ocr.orchestrator;

import com.medscope.ocr.model.OcrMetadata;
import com.medscope.ocr.model.PageAnalysis;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Complete result from OCR processing of a PDF document.
 */
@Value
@Builder
public class OcrProcessingResult {
    /**
     * Combined extracted text from all pages (digital + OCR)
     */
    String extractedText;

    /**
     * Total number of pages in document
     */
    int pageCount;

    /**
     * OCR metadata for tracking and transparency
     */
    OcrMetadata metadata;

    /**
     * Per-page analysis results
     */
    List<PageAnalysis> pageAnalyses;
}
