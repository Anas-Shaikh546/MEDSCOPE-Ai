package com.medscope.report.service;

import com.medscope.common.exception.ResourceNotFoundException;
import com.medscope.report.entity.Report;
import com.medscope.report.entity.ReportStatus;
import com.medscope.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Orchestrates upload/list/get. Never touches the filesystem directly -
 * that's FileStorageService's job (3.10). Never accepts a userId from a
 * caller - always the authenticated id resolved via @CurrentUser in the
 * controller (3.11).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final FileStorageService fileStorageService;

    /**
     * Save-file-then-DB-insert, with rollback-on-failure (3.12):
     * if the database insert fails after the physical file was written,
     * the file is deleted so we never leave an orphaned file behind.
     */
    public Report upload(Long userId, MultipartFile file) {
        StoredFile stored = fileStorageService.store(userId, file);

        try {
            Report report = Report.builder()
                    .userId(userId)
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(stored.storedFilename())
                    .filePath(stored.filePath())
                    .contentType(file.getContentType())
                    .fileSize(stored.fileSize())
                    .status(ReportStatus.UPLOADED)
                    .build();

            Report saved = reportRepository.save(report);
            log.info("Report uploaded successfully: reportId={}, userId={}", saved.getId(), userId);
            return saved;

        } catch (Exception e) {
            // DB insert failed after the file was already written - clean up
            // the orphaned file rather than leaving disk and DB inconsistent.
            fileStorageService.delete(stored.filePath());
            throw new IllegalStateException("Failed to save report metadata", e);
        }
    }

    public List<Report> listForUser(Long userId) {
        return reportRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Report-doesn't-exist and report-belongs-to-someone-else both surface
     * as the same 404 (3.19) - the caller can't distinguish "not found"
     * from "not yours", which prevents resource enumeration.
     */
    public Report getOwnedByUserOrThrow(Long reportId, Long userId) {
        return reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    /**
     * Ownership is re-verified here via getOwnedByUserOrThrow rather than
     * trusting a report already fetched elsewhere - every entry point
     * into "give me this user's report" performs its own ownership check.
     */
    public byte[] downloadFile(Long reportId, Long userId) {
        Report report = getOwnedByUserOrThrow(reportId, userId);
        return fileStorageService.read(report.getFilePath());
    }

    /**
     * Physical delete + DB delete, in that order (3.16). If the physical
     * delete fails we still remove the DB record rather than leaving the
     * user stuck with an undeletable "ghost" report - FileStorageService
     * already logs a warning internally if the file was missing.
     */
    public void delete(Long reportId, Long userId) {
        Report report = getOwnedByUserOrThrow(reportId, userId);
        fileStorageService.delete(report.getFilePath());
        reportRepository.delete(report);
        log.info("Report deleted: reportId={}, userId={}", reportId, userId);
    }
}