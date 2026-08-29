package com.medscope.report.controller;

import com.medscope.report.dto.ReportResponse;
import com.medscope.report.entity.Report;
import com.medscope.report.service.ReportService;
import com.medscope.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * All endpoints are implicitly authenticated (SecurityConfig: only
 * /auth/** and /health are public). Ownership always comes from
 * @CurrentUser, never a request parameter - see 3.11.
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ReportResponse> upload(
            @CurrentUser Long authenticatedUserId,
            @RequestParam("file") MultipartFile file
    ) {
        Report report = reportService.upload(authenticatedUserId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.from(report));
    }

    @GetMapping
    public List<ReportResponse> list(@CurrentUser Long authenticatedUserId) {
        return reportService.listForUser(authenticatedUserId).stream()
                .map(ReportResponse::from)
                .toList();
    }

    @GetMapping("/{reportId}")
    public ReportResponse getOne(
            @CurrentUser Long authenticatedUserId,
            @PathVariable Long reportId
    ) {
        Report report = reportService.getOwnedByUserOrThrow(reportId, authenticatedUserId);
        return ReportResponse.from(report);
    }

    @GetMapping("/{reportId}/file")
    public ResponseEntity<byte[]> download(
            @CurrentUser Long authenticatedUserId,
            @PathVariable Long reportId
    ) {
        Report report = reportService.getOwnedByUserOrThrow(reportId, authenticatedUserId);
        byte[] fileBytes = reportService.downloadFile(reportId, authenticatedUserId);

        // Original filename is fine to expose here (Content-Disposition) -
        // it's just what the user called their own file, not a server path.
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + report.getOriginalFilename() + "\"")
                .body(fileBytes);
    }

    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> delete(
            @CurrentUser Long authenticatedUserId,
            @PathVariable Long reportId
    ) {
        reportService.delete(reportId, authenticatedUserId);
        return ResponseEntity.noContent().build();
    }
}