package com.medscope.ocr.model;

/**
 * Classification of a PDF page's content type, used to determine
 * whether OCR processing is required.
 */
public enum PageType {
    /**
     * Page contains usable digital text layer. No OCR needed.
     */
    TEXT,

    /**
     * Page is a scanned image with no or unusable text layer. OCR required.
     */
    SCANNED,

    /**
     * Page contains both text and large images. May need OCR for image regions.
     */
    MIXED,

    /**
     * Page is essentially empty or contains only whitespace.
     */
    EMPTY
}
