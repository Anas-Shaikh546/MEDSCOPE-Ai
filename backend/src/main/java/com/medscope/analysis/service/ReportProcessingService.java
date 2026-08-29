package com.medscope.analysis.service;

import com.medscope.analysis.entity.ReportResult;
import com.medscope.analysis.extractor.ExtractedResult;
import com.medscope.analysis.extractor.ExtractedText;
import com.medscope.analysis.extractor.MedicalResultExtractor;
import com.medscope.analysis.extractor.PdfTextExtractor;
import com.medscope.analysis.extractor.TextNormalizer;
import com.medscope.analysis.repository.ReportResultRepository;
import com.medscope.report.entity.Report;
import com.medscope.report.entity.ReportStatus;
import com.medscope.report.service.FileStorageService;
import com.medscope.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates PdfTextExtractor -> TextNormalizer -> MedicalResultExtractor
 * -> ResultValidator -> database (4.2). Lives in analysis/, not
 * report/ - Step 3's ReportService stays responsible for report
 * management only (4.2 "important" note).
 *
 * Idempotency (4.16): every call deletes the report's existing results
 * and inserts a fresh set inside one transaction, so processing the
 * same report N times always ends with exactly one result set, never
 * duplicates - simpler than tracking "already processed, skip" state
 * and equally correct.
 *
 * Note: because this implementation is synchronous (no queue - Kafka
 * is explicitly out of scope per the locked rules), a report never
 * observably sits in PROCESSING from the client's point of view; it
 * moves directly from UPLOADED to PROCESSED/FAILED/UNSUPPORTED within
 * one request. The status value still exists for a future async version.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportProcessingService {

    private final ReportService reportService;
    private final FileStorageService fileStorageService;
    private final PdfTextExtractor pdfTextExtractor;
    private final TextNormalizer textNormalizer;
    private final MedicalResultExtractor medicalResultExtractor;
    private final ResultValidator resultValidator;
    private final ReportResultRepository reportResultRepository;
    private final ReportResultPersister resultPersister;

    public Report process(Long reportId, Long userId) {
        // Ownership + existence check happens exactly once, up front,
        // via the same path Step 3 already uses (4.21) - never a
        // second, looser lookup anywhere below this line.
        Report report = reportService.getOwnedByUserOrThrow(reportId, userId);

        byte[] fileBytes = fileStorageService.read(report.getFilePath());
        ExtractedText extractedText = pdfTextExtractor.extract(fileBytes);

        if (!extractedText.supported()) {
            log.info("Report has no usable text, marking UNSUPPORTED: reportId={}", reportId);
            return finalizeAs(report, ReportStatus.UNSUPPORTED, List.of());
        }

        try {
            List<String> lines = textNormalizer.toLines(extractedText.rawText());
            List<ExtractedResult> extractedResults = medicalResultExtractor.extract(lines);

            List<ReportResult> reportResults = extractedResults.stream()
                    .map(er -> resultValidator.toReportResult(reportId, er))
                    .toList();

            log.info("Report processed successfully: reportId={}, resultCount={}",
                    reportId, reportResults.size());
            return finalizeAs(report, ReportStatus.PROCESSED, reportResults);

        } catch (Exception e) {
            // Whatever failed, the old result set (if any) is still
            // cleared and the report is honestly marked FAILED rather
            // than left showing stale results under a stale status
            // (4.17 - no "20 results saved, 21st crashes, still says
            // COMPLETED").
            log.error("Report processing failed: reportId={}", reportId, e);
            finalizeAs(report, ReportStatus.FAILED, List.of());
            throw new IllegalStateException("Report processing failed", e);
        }
    }

    public List<ReportResult> getResults(Long reportId, Long userId) {
        // Ownership check only - the Report itself isn't needed by the
        // caller here, just the confirmation that this user may see it.
        reportService.getOwnedByUserOrThrow(reportId, userId);
        return reportResultRepository.findAllByReportIdOrderById(reportId);
    }

    /**
     * The only place old results are deleted and new ones (if any) are
     * inserted, together with the status update - all inside one
     * transaction (4.17), so a partial write is never visible.
     *
     * Delegated to a separate bean (ReportResultPersister) rather than
     * a @Transactional method on this class: Spring's transaction
     * proxy doesn't intercept self-invocation (this.finalizeAs(...)
     * would silently run non-transactional), so the transactional
     * boundary has to live on a real, separately-injected bean.
     */
    private Report finalizeAs(Report report, ReportStatus status, List<ReportResult> results) {
        return resultPersister.persist(report, status, results);
    }
}