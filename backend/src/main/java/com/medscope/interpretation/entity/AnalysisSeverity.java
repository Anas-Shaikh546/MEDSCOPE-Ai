package com.medscope.interpretation.entity;

/**
 * The AI's own assessment of how much attention a finding deserves -
 * deliberately a separate enum from analysis.entity.ResultStatus
 * (NORMAL/HIGH/LOW/UNKNOWN), which is a pure fact about a value vs its
 * reference range. Severity is interpretation; ResultStatus is fact.
 * They will often correlate but are never the same field or the same
 * source of truth (5.10 - facts and interpretation stay separate).
 */
public enum AnalysisSeverity {
    NORMAL,
    ATTENTION,
    CONCERN,
    URGENT
}
