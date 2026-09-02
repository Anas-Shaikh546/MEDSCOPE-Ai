package com.medscope.timeline;

import com.medscope.report.entity.Report;
import com.medscope.report.entity.ReportStatus;
import com.medscope.report.repository.ReportRepository;
import com.medscope.timeline.entity.TestCategory;
import com.medscope.timeline.entity.TestDefinition;
import com.medscope.timeline.repository.TestDefinitionRepository;
import com.medscope.user.entity.User;
import com.medscope.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the V6 migration's seed data and the new reports.test_date
 * column, against the real migrated schema (see AnalysisRelationshipTest
 * for why: Hibernate's test-profile auto-DDL never runs the migration's
 * INSERT statements, so this is the only test that can prove the seed
 * data actually exists).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
class TestDefinitionSeedDataTest {

    @Autowired private TestDefinitionRepository testDefinitionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ReportRepository reportRepository;

    @Test
    void seedDataContainsExpectedCountAndEntries() {
        long count = testDefinitionRepository.count();

        // Matches MedicalTestVocabulary.java's ~45 recognized names -
        // if this count drifts, either the migration's seed list or
        // the vocabulary changed without updating the other.
        assertEquals(43, count);

        TestDefinition hemoglobin = testDefinitionRepository.findByCanonicalName("hemoglobin").orElseThrow();
        assertEquals("Hemoglobin", hemoglobin.getDisplayName());
        assertEquals(TestCategory.CBC, hemoglobin.getCategory());
        assertEquals("g/dL", hemoglobin.getDefaultUnit());

        assertTrue(testDefinitionRepository.findByCanonicalName("hdl").isPresent());
        assertTrue(testDefinitionRepository.findByCanonicalName("tsh").isPresent());
        assertTrue(testDefinitionRepository.findByCanonicalName("not_a_real_test").isEmpty());
    }

    @Test
    void canonicalNameMustBeUnique() {
        TestDefinition duplicate = TestDefinition.builder()
                .canonicalName("hemoglobin")
                .displayName("Duplicate Hemoglobin")
                .category(TestCategory.CBC)
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> testDefinitionRepository.saveAndFlush(duplicate));
    }

    @Test
    void reportTestDateIsNullByDefaultAndPersistsWhenSet() {
        User user = userRepository.save(User.builder()
                .email("timeline-test@example.com")
                .passwordHash("hash")
                .firstName("Anas")
                .lastName("Shaikh")
                .build());

        Report withoutDate = reportRepository.save(Report.builder()
                .userId(user.getId())
                .originalFilename("a.pdf")
                .storedFilename(UUID.randomUUID() + ".pdf")
                .filePath("1/" + UUID.randomUUID() + ".pdf")
                .contentType("application/pdf")
                .fileSize(100L)
                .status(ReportStatus.UPLOADED)
                .build());

        assertNull(reportRepository.findById(withoutDate.getId()).orElseThrow().getTestDate());

        Report withDate = reportRepository.save(Report.builder()
                .userId(user.getId())
                .originalFilename("b.pdf")
                .storedFilename(UUID.randomUUID() + ".pdf")
                .filePath("1/" + UUID.randomUUID() + ".pdf")
                .contentType("application/pdf")
                .fileSize(100L)
                .status(ReportStatus.UPLOADED)
                .testDate(LocalDate.of(2026, 1, 10))
                .build());

        assertEquals(LocalDate.of(2026, 1, 10),
                reportRepository.findById(withDate.getId()).orElseThrow().getTestDate());
    }
}