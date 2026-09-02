package com.medscope.report.dto;

import com.medscope.report.entity.Report;
import com.medscope.report.entity.ReportStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Deliberately excludes filePath and storedFilename - the client never
 * needs, and must never receive, a physical filesystem detail (3.21).
 *
 * testDate is nullable - null means no confirmed lab date has been set
 * and the UI must not imply one exists (6.2). createdAt is always the
 * upload date, never the lab date.
 */
public record ReportResponse(
        Long id,
        String originalFilename,
        String contentType,
        Long fileSize,
        ReportStatus status,
        Instant createdAt,
        LocalDate testDate
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getOriginalFilename(),
                report.getContentType(),
                report.getFileSize(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getTestDate()
        );
    }
}