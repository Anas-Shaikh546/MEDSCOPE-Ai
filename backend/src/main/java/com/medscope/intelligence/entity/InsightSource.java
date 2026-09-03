package com.medscope.intelligence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Provenance link: which ReportResult row supports a given Insight.
 * report_result_id is NOT NULL at both the Java and DB level - an
 * insight source without a real backing result is rejected before
 * persistence (Task 5 InsightValidator checks this; the FK makes the
 * DB enforce it as a second line of defence).
 *
 * Traceability chain: Insight → InsightSource → ReportResult → Report → PDF
 */
@Entity
@Table(name = "insight_sources")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InsightSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "insight_id", nullable = false)
    private Long insightId;

    @Column(name = "report_result_id", nullable = false)
    private Long reportResultId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}