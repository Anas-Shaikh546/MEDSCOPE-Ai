package com.medscope.interpretation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One AI interpretation of a report's extracted facts. Never the
 * source of truth for what the report actually contains - that
 * remains com.medscope.analysis.entity.ReportResult (5.10, frozen
 * Step 4 package). This entity only holds what the AI concluded.
 *
 * uk_analyses_report (one row per report_id) is what makes
 * re-analysis idempotent (5.18) - AnalysisService replaces this row
 * rather than inserting a new one each time, same delete/replace
 * pattern as Step 4's ReportResultPersister.
 */
@Entity
@Table(name = "analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain column, not a JPA association - same reasoning as
    // Report.userId and ReportResult.reportId: every query is
    // explicitly scoped, never an implicit join.
    @Column(name = "report_id", nullable = false, unique = true)
    private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AnalysisStatus status;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    // Newline-separated for now rather than a separate table - the
    // structured part of the AI's output is the findings below;
    // recommendations are free text advice ("discuss X with a
    // doctor"), not per-fact data that needs its own relational shape.
    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    // Reproducibility (5.17) - which model/prompt combination produced
    // this analysis.
    @Column(name = "model_name")
    private String modelName;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = AnalysisStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}