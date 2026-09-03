package com.medscope.intelligence.entity;

/**
 * Lifecycle of one intelligence generation run. Independent from
 * ReportStatus (Step 4) and AnalysisStatus (Step 5) - same principle
 * as those two being kept separate: each stage has its own lifecycle.
 */
public enum InsightGenerationStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}