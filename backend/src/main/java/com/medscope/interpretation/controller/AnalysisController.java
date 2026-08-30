package com.medscope.interpretation.controller;

import com.medscope.interpretation.dto.AnalysisResponse;
import com.medscope.interpretation.service.AnalysisService;
import com.medscope.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** All interpretation reads and writes are tied to the JWT owner. */
@RestController
@RequestMapping("/interpretations")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/analyze/{reportId}")
    public AnalysisResponse analyze(@CurrentUser Long authenticatedUserId, @PathVariable Long reportId) {
        return analysisService.analyze(reportId, authenticatedUserId);
    }

    @GetMapping("/{analysisId}")
    public AnalysisResponse getById(@CurrentUser Long authenticatedUserId, @PathVariable Long analysisId) {
        return analysisService.getById(analysisId, authenticatedUserId);
    }

    @GetMapping("/by-report/{reportId}")
    public AnalysisResponse getByReport(@CurrentUser Long authenticatedUserId, @PathVariable Long reportId) {
        return analysisService.getByReportId(reportId, authenticatedUserId);
    }
}
