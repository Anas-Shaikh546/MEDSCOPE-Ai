package com.medscope.timeline.entity;

/**
 * Deliberately small, controlled set (6.15) - matches the panel
 * groupings already implied by MedicalTestVocabulary.java (Step 4).
 * Expand only when a real test genuinely doesn't fit; never invent a
 * category per-test.
 */
public enum TestCategory {
    CBC,
    LIVER,
    KIDNEY,
    THYROID,
    LIPID,
    GLUCOSE,
    VITAMINS,
    URINE,
    OTHER
}