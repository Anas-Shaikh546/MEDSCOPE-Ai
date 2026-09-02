-- Canonical test-identity layer (6.3, 6.14). Deliberately NOT linked to
-- report_results via a foreign key column - Step 4's schema stays
-- completely untouched. The join happens at query time in
-- com.medscope.timeline, matching report_results.normalized_test_name
-- against test_definitions.canonical_name.
CREATE TABLE test_definitions (
    id                  BIGSERIAL PRIMARY KEY,

    -- Matches Report Step 4's normalizedTestName values exactly
    -- (e.g. "hemoglobin", "wbc", "hdl") - this string is the bridge,
    -- not a new id column bolted onto report_results.
    canonical_name      VARCHAR(100) NOT NULL,

    display_name        VARCHAR(100) NOT NULL,
    category             VARCHAR(50) NOT NULL,
    default_unit          VARCHAR(50),

    created_at             TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_test_definitions_canonical_name UNIQUE (canonical_name)
);

-- Seeded from the exact controlled vocabulary already in
-- MedicalTestVocabulary.java (Step 4) - every normalizedTestName value
-- that vocabulary can currently produce gets a matching row here.
-- Categories per the locked, deliberately small set (6.15).
INSERT INTO test_definitions (canonical_name, display_name, category, default_unit) VALUES
    ('hemoglobin', 'Hemoglobin', 'CBC', 'g/dL'),
    ('wbc', 'WBC Count', 'CBC', '/uL'),
    ('rbc', 'RBC Count', 'CBC', 'million/uL'),
    ('platelets', 'Platelets', 'CBC', '/uL'),
    ('hematocrit', 'Hematocrit', 'CBC', '%'),
    ('mcv', 'MCV', 'CBC', 'fL'),
    ('mch', 'MCH', 'CBC', 'pg'),
    ('mchc', 'MCHC', 'CBC', 'g/dL'),
    ('neutrophils', 'Neutrophils', 'CBC', '%'),
    ('lymphocytes', 'Lymphocytes', 'CBC', '%'),
    ('monocytes', 'Monocytes', 'CBC', '%'),
    ('eosinophils', 'Eosinophils', 'CBC', '%'),
    ('basophils', 'Basophils', 'CBC', '%'),
    ('esr', 'ESR', 'CBC', 'mm/hr'),

    ('glucose', 'Glucose', 'GLUCOSE', 'mg/dL'),
    ('random_glucose', 'Random Glucose', 'GLUCOSE', 'mg/dL'),
    ('hba1c', 'HbA1c', 'GLUCOSE', '%'),

    ('total_cholesterol', 'Total Cholesterol', 'LIPID', 'mg/dL'),
    ('hdl', 'HDL', 'LIPID', 'mg/dL'),
    ('ldl', 'LDL', 'LIPID', 'mg/dL'),
    ('vldl', 'VLDL', 'LIPID', 'mg/dL'),
    ('triglycerides', 'Triglycerides', 'LIPID', 'mg/dL'),

    ('creatinine', 'Creatinine', 'KIDNEY', 'mg/dL'),
    ('urea', 'Urea', 'KIDNEY', 'mg/dL'),
    ('bun', 'BUN', 'KIDNEY', 'mg/dL'),
    ('uric_acid', 'Uric Acid', 'KIDNEY', 'mg/dL'),
    ('sodium', 'Sodium', 'KIDNEY', 'mmol/L'),
    ('potassium', 'Potassium', 'KIDNEY', 'mmol/L'),
    ('calcium', 'Calcium', 'KIDNEY', 'mg/dL'),

    ('alt', 'ALT', 'LIVER', 'U/L'),
    ('ast', 'AST', 'LIVER', 'U/L'),
    ('bilirubin_total', 'Total Bilirubin', 'LIVER', 'mg/dL'),
    ('bilirubin_direct', 'Direct Bilirubin', 'LIVER', 'mg/dL'),
    ('albumin', 'Albumin', 'LIVER', 'g/dL'),
    ('total_protein', 'Total Protein', 'LIVER', 'g/dL'),
    ('alp', 'Alkaline Phosphatase', 'LIVER', 'U/L'),

    ('tsh', 'TSH', 'THYROID', 'uIU/mL'),
    ('t3', 'T3', 'THYROID', 'ng/dL'),
    ('t4', 'T4', 'THYROID', 'ug/dL'),

    ('vitamin_d', 'Vitamin D', 'VITAMINS', 'ng/mL'),
    ('vitamin_b12', 'Vitamin B12', 'VITAMINS', 'pg/mL'),
    ('iron', 'Iron', 'VITAMINS', 'ug/dL'),
    ('ferritin', 'Ferritin', 'VITAMINS', 'ng/mL');

-- test_date is intentionally nullable and unset by any current flow -
-- no automatic date extraction in Step 6 (locked decision). Trend
-- ordering falls back to reports.created_at when this is null.
-- Never invent a medical date (6.2).
ALTER TABLE reports ADD COLUMN test_date DATE;