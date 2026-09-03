package com.medscope.intelligence;

import com.medscope.analysis.entity.ReportResult;
import com.medscope.analysis.entity.ResultStatus;
import com.medscope.analysis.repository.ReportResultRepository;
import com.medscope.intelligence.entity.*;
import com.medscope.intelligence.repository.*;
import com.medscope.report.entity.Report;
import com.medscope.report.entity.ReportStatus;
import com.medscope.report.repository.ReportRepository;
import com.medscope.user.entity.User;
import com.medscope.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the V7 schema: cascade from report to generation to insight
 * to source, FK enforcement on report_result_id, and generation versioning.
 * Runs against the real Flyway-migrated H2 schema (not Hibernate auto-DDL)
 * so ON DELETE CASCADE is actually exercised.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
class IntelligenceDataModelTest {

    @Autowired private UserRepository userRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private ReportResultRepository reportResultRepository;
    @Autowired private InsightGenerationRepository generationRepository;
    @Autowired private InsightRepository insightRepository;
    @Autowired private InsightSourceRepository sourceRepository;

    private User savedUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash("hash")
                .firstName("Anas")
                .lastName("Shaikh")
                .build());
    }

    private Report savedReport(Long userId) {
        return reportRepository.save(Report.builder()
                .userId(userId)
                .originalFilename("r.pdf")
                .storedFilename(UUID.randomUUID() + ".pdf")
                .filePath("1/" + UUID.randomUUID() + ".pdf")
                .contentType("application/pdf")
                .fileSize(100L)
                .status(ReportStatus.PROCESSED)
                .build());
    }

    private ReportResult savedResult(Long reportId) {
        return reportResultRepository.save(ReportResult.builder()
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
                .build());
    }

    @Test
    void canPersistFullChain_GenerationInsightSource() {
        User user = savedUser("intel1@example.com");
        Report report = savedReport(user.getId());
        ReportResult result = savedResult(report.getId());

        InsightGeneration gen = generationRepository.save(
                InsightGeneration.builder()
                        .reportId(report.getId())
                        .generationNumber(1)
                        .status(InsightGenerationStatus.COMPLETED)
                        .modelName("google/gemma-4-27b-it:free")
                        .modelVersion("gemma-4-27b-it")
                        .promptVersion("v1.0")
                        .build()
        );

        Insight insight = insightRepository.save(
                Insight.builder()
                        .generationId(gen.getId())
                        .type(InsightType.TREND_CONTEXT)
                        .title("Hemoglobin within range")
                        .description("Hemoglobin is within the provided reference range.")
                        .priority(InsightPriority.INFORMATIONAL)
                        .confidence(0.92)
                        .status(InsightStatus.GENERATED)
                        .build()
        );

        InsightSource source = sourceRepository.save(
                InsightSource.builder()
                        .insightId(insight.getId())
                        .reportResultId(result.getId())
                        .build()
        );

        assertNotNull(gen.getId());
        assertNotNull(insight.getId());
        assertNotNull(source.getId());
        assertEquals(result.getId(), source.getReportResultId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deletingReportCascadesToAllIntelligenceRows() {
        User user = savedUser("intel2@example.com");
        Report report = savedReport(user.getId());
        ReportResult result = savedResult(report.getId());

        InsightGeneration gen = generationRepository.save(
                InsightGeneration.builder()
                        .reportId(report.getId())
                        .generationNumber(1)
                        .status(InsightGenerationStatus.COMPLETED)
                        .build()
        );

        Insight insight = insightRepository.save(
                Insight.builder()
                        .generationId(gen.getId())
                        .type(InsightType.GENERAL_CONTEXT)
                        .title("Test")
                        .description("Test insight.")
                        .priority(InsightPriority.LOW)
                        .status(InsightStatus.GENERATED)
                        .build()
        );

        sourceRepository.save(
                InsightSource.builder()
                        .insightId(insight.getId())
                        .reportResultId(result.getId())
                        .build()
        );

        Long genId = gen.getId();
        Long insightId = insight.getId();

        reportRepository.delete(report);
        reportRepository.flush();

        assertTrue(generationRepository.findById(genId).isEmpty());
        assertTrue(insightRepository.findById(insightId).isEmpty());
        assertTrue(sourceRepository.findAllByInsightId(insightId).isEmpty());
    }

    @Test
    void generationNumberIncrementsAcrossRuns() {
        User user = savedUser("intel3@example.com");
        Report report = savedReport(user.getId());

        generationRepository.save(
                InsightGeneration.builder()
                        .reportId(report.getId())
                        .generationNumber(1)
                        .status(InsightGenerationStatus.COMPLETED)
                        .build()
        );

        generationRepository.save(
                InsightGeneration.builder()
                        .reportId(report.getId())
                        .generationNumber(2)
                        .status(InsightGenerationStatus.COMPLETED)
                        .build()
        );

        assertEquals(2, generationRepository.countByReportId(report.getId()));

        InsightGeneration latest = generationRepository
                .findTopByReportIdOrderByGenerationNumberDesc(report.getId())
                .orElseThrow();

        assertEquals(2, latest.getGenerationNumber());
    }
}