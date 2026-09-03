package com.medscope.intelligence.entity;

/**
 * How much attention this insight warrants based on the supporting
 * evidence - not a medical severity score. HIGH does not mean
 * "dangerous", it means "the evidence clearly supports paying
 * attention to this finding".
 */
public enum InsightPriority {
    HIGH,
    MODERATE,
    LOW,
    INFORMATIONAL
}