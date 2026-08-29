package com.medscope.report.entity;

/**
 * Doubles as both "report lifecycle" (Step 3) and "extraction status"
 * (Step 4), rather than maintaining two near-identical enums:
 *
 *   UPLOADED   -> pending extraction (Step 3 default)
 *   PROCESSING -> extraction in progress
 *   PROCESSED  -> extraction completed successfully
 *   FAILED     -> extraction attempted and failed
 *   UNSUPPORTED -> extraction attempted, PDF has no usable text
 *                  (e.g. a scanned image with no OCR yet)
 *   DELETED    -> reserved, not currently used by any flow
 */
public enum ReportStatus {
    UPLOADED,
    PROCESSING,
    PROCESSED,
    FAILED,
    UNSUPPORTED,
    DELETED
}