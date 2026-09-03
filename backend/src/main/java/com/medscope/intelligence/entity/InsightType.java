package com.medscope.intelligence.entity;

/**
 * What kind of pattern this insight describes. Controlled vocabulary
 * only - no arbitrary strings stored in the DB (Task 1 spec).
 */
public enum InsightType {
    TREND_CONTEXT,
    PERSISTENT_ABNORMALITY,
    SIGNIFICANT_CHANGE,
    MULTI_RESULT_PATTERN,
    FOLLOW_UP,
    GENERAL_CONTEXT
}