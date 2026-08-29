package com.medscope.report.dto;

import com.medscope.report.entity.Report;
import com.medscope.report.entity.ReportStatus;

import java.time.Instant;

/**
 * Deliberately excludes filePath and storedFilename - the client never
 * needs, and must never receive, a physical filesystem detail (3.21).
 */
public record ReportResponse(
        Long id,
        String originalFilename,
        String contentType,
        Long fileSize,
        ReportStatus status,
        Instant createdAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getOriginalFilename(),
                report.getContentType(),
                report.getFileSize(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}