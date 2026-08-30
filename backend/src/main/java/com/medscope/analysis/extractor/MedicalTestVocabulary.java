package com.medscope.analysis.extractor;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Controlled list of recognized tests (4.7). Deliberately small and
 * explicit - unrecognized test names simply get normalizedTestName=null
 * rather than a guessed mapping. Expand this map as real reports
 * demand it; never infer a mapping algorithmically.
 */
@Component
public class MedicalTestVocabulary {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            // Hemoglobin
            Map.entry("hemoglobin", "hemoglobin"),
            Map.entry("haemoglobin", "hemoglobin"),
            Map.entry("hb", "hemoglobin"),
            Map.entry("hgb", "hemoglobin"),

            // CBC - white cells
            Map.entry("wbc", "wbc"),
            Map.entry("wbc count", "wbc"),
            Map.entry("white blood cell count", "wbc"),
            Map.entry("white blood cells", "wbc"),
            Map.entry("total leukocyte count", "wbc"),
            Map.entry("tlc", "wbc"),

            // CBC - red cells
            Map.entry("rbc", "rbc"),
            Map.entry("rbc count", "rbc"),
            Map.entry("red blood cell count", "rbc"),
            Map.entry("red blood cells", "rbc"),

            Map.entry("platelets", "platelets"),
            Map.entry("platelet count", "platelets"),

            Map.entry("hematocrit", "hematocrit"),
            Map.entry("haematocrit", "hematocrit"),
            Map.entry("hct", "hematocrit"),
            Map.entry("pcv", "hematocrit"),

            Map.entry("mcv", "mcv"),
            Map.entry("mch", "mch"),
            Map.entry("mchc", "mchc"),

            // CBC - differential
            Map.entry("neutrophils", "neutrophils"),
            Map.entry("lymphocytes", "lymphocytes"),
            Map.entry("monocytes", "monocytes"),
            Map.entry("eosinophils", "eosinophils"),
            Map.entry("basophils", "basophils"),
            Map.entry("esr", "esr"),

            // Metabolic / glucose
            Map.entry("glucose", "glucose"),
            Map.entry("blood glucose", "glucose"),
            Map.entry("fasting glucose", "glucose"),
            Map.entry("fasting blood sugar", "glucose"),
            Map.entry("fbs", "glucose"),
            Map.entry("random blood sugar", "random_glucose"),
            Map.entry("rbs", "random_glucose"),
            Map.entry("hba1c", "hba1c"),
            Map.entry("glycated hemoglobin", "hba1c"),

            // Lipid panel
            Map.entry("total cholesterol", "total_cholesterol"),
            Map.entry("cholesterol total", "total_cholesterol"),
            Map.entry("cholesterol", "total_cholesterol"),
            Map.entry("hdl", "hdl"),
            Map.entry("hdl cholesterol", "hdl"),
            Map.entry("ldl", "ldl"),
            Map.entry("ldl cholesterol", "ldl"),
            Map.entry("vldl", "vldl"),
            Map.entry("triglycerides", "triglycerides"),

            // Kidney panel
            Map.entry("creatinine", "creatinine"),
            Map.entry("serum creatinine", "creatinine"),
            Map.entry("urea", "urea"),
            Map.entry("blood urea", "urea"),
            Map.entry("bun", "bun"),
            Map.entry("uric acid", "uric_acid"),
            Map.entry("sodium", "sodium"),
            Map.entry("potassium", "potassium"),
            Map.entry("calcium", "calcium"),

            // Liver panel
            Map.entry("alt", "alt"),
            Map.entry("sgpt", "alt"),
            Map.entry("ast", "ast"),
            Map.entry("sgot", "ast"),
            Map.entry("bilirubin", "bilirubin_total"),
            Map.entry("total bilirubin", "bilirubin_total"),
            Map.entry("direct bilirubin", "bilirubin_direct"),
            Map.entry("albumin", "albumin"),
            Map.entry("total protein", "total_protein"),
            Map.entry("alkaline phosphatase", "alp"),
            Map.entry("alp", "alp"),

            // Thyroid panel
            Map.entry("tsh", "tsh"),
            Map.entry("t3", "t3"),
            Map.entry("t4", "t4"),

            // Vitamins / minerals
            Map.entry("vitamin d", "vitamin_d"),
            Map.entry("vitamin b12", "vitamin_b12"),
            Map.entry("iron", "iron"),
            Map.entry("ferritin", "ferritin")
    );

    /**
     * Returns the normalized name, or null if this test name isn't in
     * the controlled vocabulary - null is a valid, expected outcome,
     * never replaced with a guess.
     */
    public String normalize(String testName) {
        if (testName == null) {
            return null;
        }
        String key = testName.trim().toLowerCase(Locale.ROOT);
        return ALIASES.get(key);
    }
}