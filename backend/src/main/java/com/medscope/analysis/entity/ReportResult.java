package com.medscope.analysis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One extracted test result from a report. Facts only - no
 * interpretation, no explanation. See Step 4 Measure 1: Step 5 consumes
 * these rows, never the PDF directly (Measure 7).
 *
 * Immutable by convention: processing replaces a report's result set
 * wholesale (delete-then-insert inside one transaction) rather than
 * updating rows in place - see ReportProcessingService (Task 4).
 */
@Entity
@Table(name = "report_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain column, not a JPA association - same reasoning as
    // Report.userId: every query is explicitly scoped, not an implicit join.
    @Column(name = "report_id", nullable = false)
    private Long reportId;

    // Exactly as printed in the report (e.g. "Hb", "Haemoglobin").
    @Column(name = "test_name", nullable = false)
    private String testName;

    // Mapped to a controlled vocabulary (e.g. "hemoglobin") when
    // recognized. Null when the extractor doesn't recognize the test -
    // never a guessed mapping.
    @Column(name = "normalized_test_name")
    private String normalizedTestName;

    // What was actually seen in the text, verbatim. Never discarded,
    // even when numericValue/textValue below are populated.
    @Column(name = "raw_value", nullable = false)
    private String rawValue;

    // Exactly one of numericValue / textValue is set, never both,
    // never neither - see MedicalResultExtractor (Task 3).
    @Column(name = "numeric_value")
    private Double numericValue;

    @Column(name = "text_value")
    private String textValue;

    // As printed - never silently converted to a different unit
    // (Step 4 Measure 4).
    @Column(name = "unit")
    private String unit;

    // Null when the report doesn't provide a range - never invented
    // (Step 4 section 4.11).
    @Column(name = "reference_low")
    private Double referenceLow;

    @Column(name = "reference_high")
    private Double referenceHigh;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ResultStatus status;

    // Deterministic-parser confidence per a fixed rule (Task 3) - not
    // fabricated precision.
    @Column(name = "confidence", nullable = false)
    private Double confidence;

    // Nullable - populated only when PDFBox can attribute the line to
    // a specific page (Step 4 Measure 2).
    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}