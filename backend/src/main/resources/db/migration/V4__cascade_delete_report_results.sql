-- Fixes a real bug: deleting a report that has already been processed
-- (i.e. has report_results rows) violated the foreign key added in
-- V3, because that FK had no ON DELETE behavior specified. Deleting
-- the results before the report is now the database's job, not
-- something report/ReportService needs to know about analysis/
-- report_results - keeps the two modules' separation intact (4.2).
ALTER TABLE report_results DROP CONSTRAINT fk_report_results_report;

ALTER TABLE report_results
    ADD CONSTRAINT fk_report_results_report
        FOREIGN KEY (report_id) REFERENCES reports (id) ON DELETE CASCADE;