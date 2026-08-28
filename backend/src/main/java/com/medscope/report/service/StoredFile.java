package com.medscope.report.service;

/**
 * Result of physically saving a file to storage - what ReportService
 * needs to build a Report entity, nothing more.
 */
public record StoredFile(
        String storedFilename,
        String filePath,
        long fileSize
) {
}