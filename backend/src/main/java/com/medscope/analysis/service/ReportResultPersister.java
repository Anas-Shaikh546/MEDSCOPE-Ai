package com.medscope.analysis.service;

import com.medscope.analysis.entity.ReportResult;
import com.medscope.analysis.repository.ReportResultRepository;
import com.medscope.report.entity.Report;
import com.medscope.report.entity.ReportStatus;
import com.medscope.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The sole @Transactional boundary for "replace this report's results
 * and update its status" (4.17). Kept as its own bean, called through
 * Spring's proxy from ReportProcessingService, rather than a
 * @Transactional method on that class - a same-class call would bypass
 * the proxy and silently run non-transactional.
 */
@Component
@RequiredArgsConstructor
public class ReportResultPersister {

    private final ReportRepository reportRepository;
    private final ReportResultRepository reportResultRepository;

    @Transactional
    public Report persist(Report report, ReportStatus status, List<ReportResult> results) {
        reportResultRepository.deleteAllByReportId(report.getId());
        if (!results.isEmpty()) {
            reportResultRepository.saveAll(results);
        }
        report.setStatus(status);
        return reportRepository.save(report);
    }
}