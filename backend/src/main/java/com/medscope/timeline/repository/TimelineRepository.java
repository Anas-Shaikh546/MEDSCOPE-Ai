package com.medscope.timeline.repository;

import com.medscope.analysis.entity.ReportResult;
import com.medscope.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Read-only view over the Step 4 data: never writes to report_results
 * or reports, never adds columns to them. Step 4's schema stays frozen.
 *
 * The bridge from ReportResult to TestDefinition is the string match
 * rr.normalizedTestName == canonicalName, done at the query layer, not
 * as a FK column on report_results (locked decision, 6.3/6.14).
 *
 * Extends JpaRepository<ReportResult, Long> so Spring wires it up
 * cleanly, but only read-only custom queries are ever called from
 * TimelineService - no save/delete methods.
 */
public interface TimelineRepository extends JpaRepository<ReportResult, Long> {

    /**
     * All distinct canonical test names for which this user has at
     * least one extracted result. Used to build the "what tests can
     * I show trends for?" list without scanning every report.
     * Only returns rows where normalizedTestName is not null (unrecognized
     * test names have no canonical identity and can't be trended).
     */
    @Query("""
            SELECT DISTINCT rr.normalizedTestName
            FROM ReportResult rr
            JOIN Report r ON r.id = rr.reportId
            WHERE r.userId = :userId
              AND rr.normalizedTestName IS NOT NULL
              AND rr.numericValue IS NOT NULL
            ORDER BY rr.normalizedTestName
            """)
    List<String> findDistinctCanonicalNamesForUser(@Param("userId") Long userId);

    /**
     * All numeric results for one canonical test name, scoped to the
     * authenticated user. Returns both testDate and createdAt so
     * TimelineService can apply the fallback ordering in Java
     * (testDate if non-null, else createdAt) rather than relying on
     * COALESCE across two different temporal types in JPQL, which
     * isn't portable across JPA implementations.
     *
     * Source traceability fields (reportId, reportResultId) are included
     * so the caller can let the user click through to the original
     * report (6.16).
     *
     * Returns Object[] rows: [rr.reportId, rr.id, rr.numericValue,
     * rr.unit, r.testDate, r.createdAt, rr.referenceLow,
     * rr.referenceHigh, rr.status].
     */
    @Query("""
            SELECT rr.reportId, rr.id, rr.numericValue, rr.unit,
                   r.testDate, r.createdAt,
                   rr.referenceLow, rr.referenceHigh, rr.status
            FROM ReportResult rr
            JOIN Report r ON r.id = rr.reportId
            WHERE r.userId = :userId
              AND rr.normalizedTestName = :canonicalName
              AND rr.numericValue IS NOT NULL
            """)
    List<Object[]> findObservationsForUserAndTest(
            @Param("userId") Long userId,
            @Param("canonicalName") String canonicalName);
}