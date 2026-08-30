CREATE TABLE analyses (
    id                  BIGSERIAL PRIMARY KEY,
    report_id           BIGINT NOT NULL,

    status              VARCHAR(20) NOT NULL,
    summary             TEXT,
    recommendations     TEXT,

    -- Reproducibility (5.17) - which model/prompt produced this
    -- analysis, so the same report can be re-analyzed later and we
    -- can tell whether a different result came from different input
    -- or a different model/prompt.
    model_name          VARCHAR(100),
    model_version       VARCHAR(100),
    prompt_version      VARCHAR(50),

    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    -- One active analysis per report (5.18) - enforced at the DB
    -- level, not just in application logic. Re-analysis replaces
    -- this row rather than the service needing to check-then-insert.
    CONSTRAINT uk_analyses_report UNIQUE (report_id),

    -- Cascade from the start this time (the Step 4 report_results FK
    -- originally lacked this and had to be patched in V4).
    CONSTRAINT fk_analyses_report
        FOREIGN KEY (report_id) REFERENCES reports (id) ON DELETE CASCADE
);

CREATE TABLE analysis_findings (
    id                  BIGSERIAL PRIMARY KEY,
    analysis_id         BIGINT NOT NULL,

    -- Grounds every finding in an actual extracted fact - a finding
    -- can never exist without pointing at the specific report_results
    -- row it interprets (5.10, 5.16: AI must not invent results).
    report_result_id    BIGINT NOT NULL,

    interpretation       TEXT NOT NULL,
    severity              VARCHAR(20) NOT NULL,

    created_at             TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_analysis_findings_analysis
        FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE CASCADE,

    CONSTRAINT fk_analysis_findings_report_result
        FOREIGN KEY (report_result_id) REFERENCES report_results (id) ON DELETE CASCADE
);

CREATE INDEX idx_analysis_findings_analysis_id ON analysis_findings (analysis_id);