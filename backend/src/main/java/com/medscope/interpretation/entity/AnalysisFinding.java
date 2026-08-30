package com.medscope.interpretation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One AI interpretation of one extracted fact. reportResultId is
 * required (not nullable) on purpose: a finding must always trace
 * back to a real ReportResult row - the AI cannot produce a "floating"
 * finding about a test that was never actually extracted (5.10, 5.16).
 */
@Entity
@Table(name = "analysis_findings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "analysis_id", nullable = false)
    private Long analysisId;

    // Points at the specific fact this finding interprets - never a
    // test name string the AI made up, always a real row id that
    // existed before the AI saw anything (see AnalysisService, Task 4).
    @Column(name = "report_result_id", nullable = false)
    private Long reportResultId;

    @Column(name = "interpretation", nullable = false, columnDefinition = "TEXT")
    private String interpretation;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AnalysisSeverity severity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}