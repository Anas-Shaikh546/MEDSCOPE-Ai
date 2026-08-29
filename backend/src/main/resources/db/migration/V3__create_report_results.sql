CREATE TABLE report_results (
    id                     BIGSERIAL PRIMARY KEY,
    report_id              BIGINT NOT NULL,

    test_name              VARCHAR(255) NOT NULL,
    normalized_test_name   VARCHAR(100),

    -- Never discard what was actually extracted, even if we can't
    -- parse it into a number (e.g. "Negative", "<5", "Trace").
    raw_value               VARCHAR(255) NOT NULL,

    -- Exactly one of these two is populated, never both/neither -
    -- see ReportResult entity. Qualitative tests (HIV: Non-reactive)
    -- are not forced into a numeric column.
    numeric_value           DOUBLE PRECISION,
    text_value               VARCHAR(255),

    unit                     VARCHAR(50),

    -- Null when the report itself doesn't provide a range - never
    -- guessed/invented by the application.
    reference_low            DOUBLE PRECISION,
    reference_high            DOUBLE PRECISION,

    -- Relative to the report's own reference range only. Not a
    -- diagnosis - see ResultStatus.
    status                    VARCHAR(20) NOT NULL,

    confidence                DOUBLE PRECISION NOT NULL,

    -- Nullable - populated when PDFBox can report which page a line
    -- of text came from. Improves traceability for Step 5.
    page_number                INTEGER,

    created_at                 TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_report_results_report
        FOREIGN KEY (report_id) REFERENCES reports (id)
);

-- Nearly every future query is WHERE report_id = ... (via an
-- already-ownership-verified Report), same reasoning as reports.user_id.
CREATE INDEX idx_report_results_report_id ON report_results (report_id);