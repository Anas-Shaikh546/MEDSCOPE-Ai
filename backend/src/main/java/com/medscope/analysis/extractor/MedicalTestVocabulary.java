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
            Map.entry("hemoglobin", "hemoglobin"),
            Map.entry("haemoglobin", "hemoglobin"),
            Map.entry("hb", "hemoglobin"),
            Map.entry("hgb", "hemoglobin"),

            Map.entry("wbc", "wbc"),
            Map.entry("wbc count", "wbc"),
            Map.entry("white blood cell count", "wbc"),
            Map.entry("white blood cells", "wbc"),

            Map.entry("platelets", "platelets"),
            Map.entry("platelet count", "platelets"),

            Map.entry("glucose", "glucose"),
            Map.entry("blood glucose", "glucose"),
            Map.entry("fasting glucose", "glucose"),

            Map.entry("total cholesterol", "total_cholesterol"),
            Map.entry("cholesterol total", "total_cholesterol"),
            Map.entry("cholesterol", "total_cholesterol"),

            Map.entry("hdl", "hdl"),
            Map.entry("hdl cholesterol", "hdl"),

            Map.entry("ldl", "ldl"),
            Map.entry("ldl cholesterol", "ldl"),

            Map.entry("triglycerides", "triglycerides")
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