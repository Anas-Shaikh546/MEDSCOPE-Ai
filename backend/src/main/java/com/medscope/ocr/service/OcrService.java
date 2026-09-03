package com.medscope.ocr.service;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * OCR service abstraction. Allows switching OCR engines without
 * changing MedScope's extraction logic.
 *
 * Recommended implementation: PaddleOCR for medical reports
 * (handles tables, columns, small fonts, structured data).
 */
public interface OcrService {

    /**
     * Perform OCR on a single page image.
     *
     * @param image Page rendered as BufferedImage
     * @param pageNumber Zero-based page index
     * @return OCR result with text, confidence, and layout information
     * @throws IOException if OCR processing fails
     */
    OcrResult processPage(BufferedImage image, int pageNumber) throws IOException;

    /**
     * Get OCR engine name for metadata tracking.
     */
    String getEngineName();

    /**
     * Get OCR engine version for metadata tracking.
     */
    String getEngineVersion();
}
