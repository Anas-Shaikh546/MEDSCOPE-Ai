package com.medscope.analysis.controller;

import com.medscope.analysis.dto.ReportResultResponse;
import com.medscope.analysis.dto.ReportResultsResponse;
import com.medscope.analysis.entity.ReportResult;
import com.medscope.analysis.service.ReportProcessingService;
import com.medscope.report.dto.ReportResponse;
import com.medscope.report.entity.Report;
import com.medscope.report.service.ReportService;
import com.medscope.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Separate from ReportController on purpose (4.2, 4.14): upload/list/
 * download/delete is report management, this is processing. Same
 * /reports/{id} base path, same ownership pattern (4.21) - JWT ->
 * @CurrentUser -> reportId + userId together, never reportId alone.
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportProcessingController {

    private final ReportProcessingService reportProcessingService;
    private final ReportService reportService;

    @PostMapping("/{reportId}/process")
    public ReportResponse process(
            @CurrentUser Long authenticatedUserId,
            @PathVariable Long reportId
    ) {
        Report report = reportProcessingService.process(reportId, authenticatedUserId);
        return ReportResponse.from(report);
    }

    @GetMapping("/{reportId}/results")
    public ReportResultsResponse getResults(
            @CurrentUser Long authenticatedUserId,
            @PathVariable Long reportId
    ) {
        // Ownership re-verified here too (getOwnedByUserOrThrow), same
        // as every other report/{id}/... endpoint - status is read
        // from the report itself so the response is always internally
        // consistent (an empty results list means something different
        // for UNSUPPORTED than for "not processed yet").
        Report report = reportService.getOwnedByUserOrThrow(reportId, authenticatedUserId);
        List<ReportResult> results = reportProcessingService.getResults(reportId, authenticatedUserId);

        return new ReportResultsResponse(
                reportId,
                report.getStatus(),
                results.stream().map(ReportResultResponse::from).toList()
        );
    }
}