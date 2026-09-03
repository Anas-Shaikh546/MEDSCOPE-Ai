-- Step 7 intelligence layer. Three tables, four cascade rules, all with
-- ON DELETE CASCADE from the start (lesson from Step 4's V4 patch).
--
-- Relationship chain:
--   Report → InsightGeneration → Insight → InsightSource → ReportResult
--
-- InsightGeneration is the versioning record: one per report per run.
-- Regenerating replaces its child Insights (and their Sources) wholesale,
-- same delete-then-reinsert pattern used in Step 4 (ReportResultPersister)
-- and Step 5 (AnalysisService). The generation record itself is retained
-- so the history of runs is preserved even after the child rows are replaced.

CREATE TABLE insight_generations (
    id                  BIGSERIAL PRIMARY KEY,
    report_id           BIGINT NOT NULL,

    -- Which run this represents. Starts at 1, incremented each time
    -- the user regenerates insights for the same report.
    generation_number   INTEGER NOT NULL DEFAULT 1,

    status              VARCHAR(20) NOT NULL,

    -- Reproducibility: same fields as analyses (Step 5) so future
    -- comparisons across runs are possible (Step 8).
    model_name          VARCHAR(100),
    model_version       VARCHAR(100),
    prompt_version      VARCHAR(50),

    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_insight_generations_report
        FOREIGN KEY (report_id) REFERENCES reports (id) ON DELETE CASCADE
);

-- Most queries will be "latest generation for this user's report".
CREATE INDEX idx_insight_generations_report_id ON insight_generations (report_id);

CREATE TABLE insights (
    id                  BIGSERIAL PRIMARY KEY,
    generation_id       BIGINT NOT NULL,

    -- Controlled vocabulary only - no arbitrary strings.
    -- Values: TREND_CONTEXT, PERSISTENT_ABNORMALITY, SIGNIFICANT_CHANGE,
    --         MULTI_RESULT_PATTERN, FOLLOW_UP, GENERAL_CONTEXT
    type                VARCHAR(50) NOT NULL,

    title               VARCHAR(255) NOT NULL,
    description         TEXT NOT NULL,

    -- Values: HIGH, MODERATE, LOW, INFORMATIONAL
    priority            VARCHAR(20) NOT NULL,

    -- Follow-up questions the user might want to discuss with a clinician.
    -- Newline-separated free text - not medical instructions.
    follow_up_questions TEXT,

    -- Extraction-confidence-style score from the AI (0.0-1.0).
    -- Not medical certainty - see UI note in spec section 12.
    confidence          DOUBLE PRECISION,

    -- Values: GENERATED, VALIDATED, FAILED, DISMISSED
    status              VARCHAR(20) NOT NULL DEFAULT 'GENERATED',

    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_insights_generation
        FOREIGN KEY (generation_id) REFERENCES insight_generations (id) ON DELETE CASCADE
);

CREATE INDEX idx_insights_generation_id ON insights (generation_id);
CREATE INDEX idx_insights_priority ON insights (priority);

CREATE TABLE insight_sources (
    id                  BIGSERIAL PRIMARY KEY,
    insight_id          BIGINT NOT NULL,

    -- Every source must point at a real ReportResult row - never a
    -- fabricated result ID. InsightValidator checks this before
    -- persisting (Task 5). The FK makes the DB enforce it too.
    report_result_id    BIGINT NOT NULL,

    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_insight_sources_insight
        FOREIGN KEY (insight_id) REFERENCES insights (id) ON DELETE CASCADE,

    CONSTRAINT fk_insight_sources_report_result
        FOREIGN KEY (report_result_id) REFERENCES report_results (id) ON DELETE CASCADE
);

CREATE INDEX idx_insight_sources_insight_id ON insight_sources (insight_id);