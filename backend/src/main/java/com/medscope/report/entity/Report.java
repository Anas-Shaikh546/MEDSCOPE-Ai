package com.medscope.report.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Metadata only. The actual PDF bytes live on disk under
 * uploads/{userId}/{storedFilename} - see FileStorageService (Task 3+).
 * Never put the file contents in this entity or this table.
 */
@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Owning user's id only - never a User association/join here.
    // Every query against this table must filter by this column
    // (findByIdAndUserId, findAllByUserId) rather than trusting a
    // client-supplied id.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // What the user called it. Metadata only - never used to build a
    // filesystem path.
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    // Server-generated (UUID-based) name actually used on disk.
    // originalFilename != storedFilename, always.
    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFilename;

    // Path relative to the configured upload root - resolved/normalized
    // and verified to stay inside that root before ever being used.
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Step 6 addition. Nullable and unset by any current flow - no
    // automatic date extraction yet (locked decision). Never confuse
    // this with createdAt: createdAt is when the user uploaded the
    // file, testDate is when the lab test actually happened (6.2).
    // The timeline package's ordering falls back to createdAt when
    // this is null, and must label results accordingly rather than
    // implying a confirmed test date exists.
    @Column(name = "test_date")
    private LocalDate testDate;

    // OCR metadata for transparency (Phase 7)
    @Column(name = "ocr_used")
    private Boolean ocrUsed;

    @Column(name = "ocr_confidence")
    private Double ocrConfidence;

    @Column(name = "ocr_pages")
    private String ocrPages; // Comma-separated page numbers

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ReportStatus.UPLOADED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}