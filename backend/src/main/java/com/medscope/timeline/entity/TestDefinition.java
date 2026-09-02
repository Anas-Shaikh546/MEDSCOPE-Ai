package com.medscope.timeline.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * The canonical identity a test is grouped/trended by. Bridges to
 * Step 4's frozen ReportResult via canonicalName == normalizedTestName
 * (a query-time string match, not a foreign key) - see 6.3, 6.14, and
 * the locked decision: report_results gets no new column for this.
 */
@Entity
@Table(name = "test_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Must exactly match a value MedicalTestVocabulary.java can
    // produce as normalizedTestName - this string is the bridge to
    // Step 4, not an arbitrary label.
    @Column(name = "canonical_name", nullable = false, unique = true)
    private String canonicalName;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private TestCategory category;

    @Column(name = "default_unit")
    private String defaultUnit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}