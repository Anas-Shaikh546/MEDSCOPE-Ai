package com.medscope.interpretation.entity;

/**
 * Independent from report/entity/ReportStatus on purpose (5.2, locked
 * decision): extraction and AI interpretation are two separate
 * lifecycles. A report can be PROCESSED (Step 4 succeeded) while its
 * analysis is FAILED (Step 5 failed) - that's a valid, expected state,
 * not a contradiction.
 */
public enum AnalysisStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}