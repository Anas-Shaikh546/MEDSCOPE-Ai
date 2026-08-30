package com.medscope.interpretation;

import com.medscope.analysis.entity.ReportResult;
import com.medscope.analysis.entity.ResultStatus;
import com.medscope.analysis.repository.ReportResultRepository;
import com.medscope.interpretation.entity.Analysis;
import com.medscope.interpretation.entity.AnalysisFinding;
import com.medscope.interpretation.entity.AnalysisSeverity;
import com.medscope.interpretation.entity.AnalysisStatus;
import com.medscope.interpretation.repository.AnalysisFindingRepository;
import com.medscope.interpretation.repository.AnalysisRepository;
import com.medscope.report.entity.Report;
import com.medscope.report.entity.ReportStatus;
import com.medscope.report.repository.ReportRepository;
import com.medscope.user.entity.User;
import com.medscope.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves the Task 1 schema/entities actually behave correctly under
 * real persistence, not just that they compile: cascade delete from
 * reports all the way down to analysis_findings, and the one-analysis-
 * per-report constraint (5.18 idempotency groundwork).
 *
 * Deliberately overrides the test profile's usual Hibernate-inferred
 * schema (ddl-auto=create-drop, Flyway disabled) and runs the real
 * Flyway migrations instead. Hibernate auto-DDL has no idea about
 * ON DELETE CASCADE - that only exists in the migration SQL - so a
 * cascade test against the inferred schema would pass or fail for the
 * wrong reason. This exposed a real gap: V4's report_results cascade
 * fix (Step 4) was never actually exercised against a real schema by
 * any existing test either.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
class AnalysisRelationshipTest {

    @Autowired private UserRepository userRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private ReportResultRepository reportResultRepository;
    @Autowired private AnalysisRepository analysisRepository;
    @Autowired private AnalysisFindingRepository analysisFindingRepository;
    @Autowired private EntityManager entityManager;

    private User persistUser(String email) {
        User user = User.builder()
                .email(email)
                .passwordHash("hash")
                .firstName("Anas")
                .lastName("Shaikh")
                .build();
        return userRepository.save(user);
    }

    private Report persistReport(Long userId) {
        Report report = Report.builder()
                .userId(userId)
                .originalFilename("report.pdf")
                .storedFilename(java.util.UUID.randomUUID() + ".pdf")
                .filePath("1/" + java.util.UUID.randomUUID() + ".pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .status(ReportStatus.PROCESSED)
                .build();
        return reportRepository.save(report);
    }

    private ReportResult persistReportResult(Long reportId) {
        ReportResult result = ReportResult.builder()
                .reportId(reportId)
                .testName("Hemoglobin")
                .normalizedTestName("hemoglobin")
                .rawValue("13.8")
                .numericValue(13.8)
                .unit("g/dL")
                .referenceLow(13.0)
                .referenceHigh(17.0)
                .status(ResultStatus.NORMAL)
                .confidence(0.95)
                .build();
        return reportResultRepository.save(result);
    }

    @Test
    void canPersistAnalysisWithFindingLinkedToRealReportResult() {
        User user = persistUser("interp1@example.com");
        Report report = persistReport(user.getId());
        ReportResult result = persistReportResult(report.getId());

        Analysis analysis = analysisRepository.save(Analysis.builder()
                .reportId(report.getId())
                .status(AnalysisStatus.COMPLETED)
                .summary("Overall results are within range.")
                .modelName("claude")
                .modelVersion("claude-sonnet-5")
                .promptVersion("v1")
                .build());

        AnalysisFinding finding = analysisFindingRepository.save(AnalysisFinding.builder()
                .analysisId(analysis.getId())
                .reportResultId(result.getId())
                .interpretation("Within the reference range provided in the report.")
                .severity(AnalysisSeverity.NORMAL)
                .build());

        assertNotNull(analysis.getId());
        assertEquals(report.getId(), analysisRepository.findByReportId(report.getId()).orElseThrow().getReportId());
        assertEquals(1, analysisFindingRepository.findAllByAnalysisIdOrderById(analysis.getId()).size());
        assertEquals(result.getId(), finding.getReportResultId());
    }

    @Test
    void deletingReportCascadesToAnalysisAndFindings() {
        User user = persistUser("interp2@example.com");
        Report report = persistReport(user.getId());
        ReportResult result = persistReportResult(report.getId());

        Analysis analysis = analysisRepository.save(Analysis.builder()
                .reportId(report.getId())
                .status(AnalysisStatus.COMPLETED)
                .build());

        analysisFindingRepository.save(AnalysisFinding.builder()
                .analysisId(analysis.getId())
                .reportResultId(result.getId())
                .interpretation("Some interpretation.")
                .severity(AnalysisSeverity.NORMAL)
                .build());

        Long analysisId = analysis.getId();

        reportRepository.delete(report);
        reportRepository.flush();

        // Clear persistence context to see actual database state after cascade delete
        entityManager.clear();

        assertTrue(analysisRepository.findById(analysisId).isEmpty());
        assertTrue(analysisFindingRepository.findAllByAnalysisIdOrderById(analysisId).isEmpty());
    }

    @Test
    void secondAnalysisForSameReport_violatesUniqueConstraint() {
        User user = persistUser("interp3@example.com");
        Report report = persistReport(user.getId());

        analysisRepository.saveAndFlush(Analysis.builder()
                .reportId(report.getId())
                .status(AnalysisStatus.COMPLETED)
                .build());

        Analysis duplicate = Analysis.builder()
                .reportId(report.getId())
                .status(AnalysisStatus.COMPLETED)
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> analysisRepository.saveAndFlush(duplicate));
    }
}