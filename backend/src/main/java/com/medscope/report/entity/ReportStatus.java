package com.medscope.report.entity;

/**
 * Step 3 only ever produces UPLOADED. The rest are reserved for Step 4+
 * (processing/analysis) so we don't pretend that pipeline exists yet.
 */
public enum ReportStatus {
    UPLOADED,
    PROCESSING,
    PROCESSED,
    FAILED,
    DELETED
}