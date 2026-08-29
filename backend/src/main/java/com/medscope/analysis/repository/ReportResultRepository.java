package com.medscope.analysis.repository;

import com.medscope.analysis.entity.ReportResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Every method is scoped by reportId, mirroring ReportRepository's
 * pattern - by the time this is called, ownership has already been
 * verified via ReportService.getOwnedByUserOrThrow (see
 * ReportProcessingService), so reportId alone is safe to query by here.
 */
public interface ReportResultRepository extends JpaRepository<ReportResult, Long> {

    List<ReportResult> findAllByReportIdOrderById(Long reportId);

    void deleteAllByReportId(Long reportId);
}