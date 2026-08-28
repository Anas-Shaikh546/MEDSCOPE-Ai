package com.medscope.report.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over "where report files physically live". ReportService
 * only ever talks to this interface - never java.nio.file.Files directly.
 *
 * Today: LocalFileStorageService (local disk). If this ever becomes
 * S3FileStorageService, ReportService does not change.
 */
public interface FileStorageService {

    /**
     * Validates and persists the file for the given user, returning the
     * server-generated identity needed to build a Report entity.
     * Throws on any validation failure (bad extension, bad MIME type,
     * bad PDF signature, empty file, oversized file).
     */
    StoredFile store(Long userId, MultipartFile file);

    /**
     * Loads the raw bytes for an already-stored, already-owner-verified
     * file. Callers must have already confirmed ownership via
     * ReportRepository#findByIdAndUserId before calling this.
     */
    byte[] read(String filePath);

    /**
     * Deletes the physical file. Safe to call even if the file is
     * already missing (Task 4 uses this for both normal delete and
     * upload-failure cleanup).
     */
    void delete(String filePath);
}