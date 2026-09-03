package com.medscope.intelligence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One intelligence generation run for a report. Retained across
 * re-generations so the history of runs is preserved even after child
 * Insight rows are replaced wholesale (decision #4).
 *
 * The versioning contract: each new run increments generation_number.
 * Child insights are deleted and re-inserted inside one transaction
 * (same pattern as ReportResultPersister / Step 4). The generation
 * record itself is never deleted on re-run - only on report deletion
 * (ON DELETE CASCADE from reports).
 */
@Entity
@Table(name = "insight_generations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InsightGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain column, not a JPA association - same pattern as every
    // other per-report entity in this codebase.
    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "generation_number", nullable = false)
    private Integer generationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InsightGenerationStatus status;

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
            this.status = InsightGenerationStatus.PENDING;
        }
        if (this.generationNumber == null) {
            this.generationNumber = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}