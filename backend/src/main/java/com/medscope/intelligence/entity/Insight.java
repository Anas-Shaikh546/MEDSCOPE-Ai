package com.medscope.intelligence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One AI-generated insight, grounded in real evidence (InsightSource
 * rows pointing at actual ReportResult ids). Never created without
 * at least one InsightSource - the validator enforces this before
 * persistence (Task 5).
 */
@Entity
@Table(name = "insights")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Insight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "generation_id", nullable = false)
    private Long generationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private InsightType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private InsightPriority priority;

    // Newline-separated questions the user might want to raise with a
    // clinician. Not medical instructions - see spec section 5 (Task 6).
    @Column(name = "follow_up_questions", columnDefinition = "TEXT")
    private String followUpQuestions;

    // AI's own confidence score - not medical certainty (spec section 12).
    @Column(name = "confidence")
    private Double confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InsightStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) {
            this.status = InsightStatus.GENERATED;
        }
    }
}