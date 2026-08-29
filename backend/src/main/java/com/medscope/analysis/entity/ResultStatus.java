package com.medscope.analysis.entity;

/**
 * Describes a result's relationship to the reference range printed in
 * the report the value came from - nothing more. This is NOT a medical
 * diagnosis or interpretation (see Step 4 spec 4.12 / Measure 1 - facts
 * vs interpretation is the boundary Step 5 depends on).
 */
public enum ResultStatus {
    NORMAL,
    HIGH,
    LOW,

    // No reference range was available (report didn't provide one, or
    // the result is qualitative) - never guessed.
    UNKNOWN
}