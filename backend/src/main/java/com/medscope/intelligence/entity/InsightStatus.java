package com.medscope.intelligence.entity;

/**
 * Per-insight lifecycle. GENERATED = produced by AI and passed
 * validation. VALIDATED = optionally confirmed by a future review
 * layer (Step 8+). FAILED = AI produced it but it failed validation
 * (e.g. source result IDs didn't exist). DISMISSED = user chose to
 * hide it (future UI feature).
 */
public enum InsightStatus {
    GENERATED,
    VALIDATED,
    FAILED,
    DISMISSED
}