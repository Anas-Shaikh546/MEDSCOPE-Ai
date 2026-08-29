package com.medscope.analysis.extractor;

/**
 * Result of attempting text extraction from a PDF.
 * supported=false means "this PDF has no usable text" (e.g. a scanned
 * image with no OCR yet) - the caller must honestly mark the report
 * UNSUPPORTED rather than pretending extraction produced something (4.4).
 */
public record ExtractedText(
        boolean supported,
        String rawText,
        int pageCount
) {
    public static ExtractedText unsupported(int pageCount) {
        return new ExtractedText(false, "", pageCount);
    }

    public static ExtractedText of(String rawText, int pageCount) {
        return new ExtractedText(true, rawText, pageCount);
    }
}