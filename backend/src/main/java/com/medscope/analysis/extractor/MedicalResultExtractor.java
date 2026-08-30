package com.medscope.analysis.extractor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, pattern-based extraction only - no AI/LLM involved
 * here (Step 4 is facts, not interpretation - see spec Measure 1).
 *
 * Recognizes lines of the form:
 *   "Hemoglobin 13.8 g/dL 13.0 - 17.0"    (numeric + unit + two-sided range)
 *   "Total Cholesterol 185 mg/dL < 200"    (numeric + unit + upper threshold)
 *   "HDL 55 mg/dL > 40"                     (numeric + unit + lower threshold)
 *   "Glucose 95 mg/dL"                       (numeric + unit, no range)
 *   "HIV Non-reactive"                        (qualitative)
 *
 * Confidence is assigned by a fixed rule based on which pattern matched
 * and whether the test name is in the controlled vocabulary - not
 * fabricated precision (4.13).
 */
@Component
@RequiredArgsConstructor
public class MedicalResultExtractor {

    private static final String NUM = "[\\d,]+(?:\\.\\d+)?";
    private static final String UNIT = "[A-Za-z%µ/^0-9]+";

    // "TestName  123.4 unit  10.0 - 20.0"
    private static final Pattern NUMERIC_WITH_RANGE = Pattern.compile(
            "^([A-Za-z][A-Za-z0-9 ]*?)\\s+(" + NUM + ")\\s*(" + UNIT + ")?\\s+(" + NUM + ")\\s*-\\s*(" + NUM + ")$"
    );

    // "TestName  123.4 unit  < 200" - only an upper bound is given
    // (e.g. desirable cholesterol is "below 200").
    private static final Pattern NUMERIC_UPPER_THRESHOLD = Pattern.compile(
            "^([A-Za-z][A-Za-z0-9 ]*?)\\s+(" + NUM + ")\\s*(" + UNIT + ")?\\s*<\\s*(" + NUM + ")$"
    );

    // "TestName  123.4 unit  > 40" - only a lower bound is given
    // (e.g. desirable HDL is "above 40").
    private static final Pattern NUMERIC_LOWER_THRESHOLD = Pattern.compile(
            "^([A-Za-z][A-Za-z0-9 ]*?)\\s+(" + NUM + ")\\s*(" + UNIT + ")?\\s*>\\s*(" + NUM + ")$"
    );

    // "TestName  123.4 unit" (no reference range provided)
    private static final Pattern NUMERIC_NO_RANGE = Pattern.compile(
            "^([A-Za-z][A-Za-z0-9 ]*?)\\s+(" + NUM + ")\\s*(" + UNIT + ")?$"
    );

    // "TestName  Non-reactive" / "TestName  Positive" / etc.
    private static final Pattern QUALITATIVE = Pattern.compile(
            "^([A-Za-z][A-Za-z0-9 ]*?)\\s+(Negative|Positive|Non-reactive|Reactive|Trace|Normal|Abnormal)$",
            Pattern.CASE_INSENSITIVE
    );

    private final MedicalTestVocabulary vocabulary;

    public List<ExtractedResult> extract(List<String> lines) {
        List<ExtractedResult> results = new ArrayList<>();

        for (String line : lines) {
            ExtractedResult result = tryMatch(line);
            if (result != null) {
                results.add(result);
            }
        }

        return results;
    }

    private ExtractedResult tryMatch(String line) {
        Matcher rangeMatch = NUMERIC_WITH_RANGE.matcher(line);
        if (rangeMatch.matches()) {
            return buildNumericWithRange(rangeMatch);
        }

        Matcher upperThresholdMatch = NUMERIC_UPPER_THRESHOLD.matcher(line);
        if (upperThresholdMatch.matches()) {
            return buildNumericThreshold(upperThresholdMatch, true);
        }

        Matcher lowerThresholdMatch = NUMERIC_LOWER_THRESHOLD.matcher(line);
        if (lowerThresholdMatch.matches()) {
            return buildNumericThreshold(lowerThresholdMatch, false);
        }

        Matcher noRangeMatch = NUMERIC_NO_RANGE.matcher(line);
        if (noRangeMatch.matches()) {
            return buildNumericNoRange(noRangeMatch);
        }

        Matcher qualitativeMatch = QUALITATIVE.matcher(line);
        if (qualitativeMatch.matches()) {
            return buildQualitative(qualitativeMatch);
        }

        return null;
    }

    private ExtractedResult buildNumericWithRange(Matcher m) {
        String testName = m.group(1).trim();
        String rawValue = m.group(2);
        String unit = emptyToNull(m.group(3));
        Double low = parseNumber(m.group(4));
        Double high = parseNumber(m.group(5));
        Double value = parseNumber(rawValue);

        if (value == null || low == null || high == null) {
            return null;
        }

        String normalized = vocabulary.normalize(testName);
        double confidence = baseConfidence(0.90, normalized);

        return new ExtractedResult(testName, normalized, rawValue, value, null, unit, low, high, confidence);
    }

    /**
     * isUpperBound=true for "< threshold" (referenceHigh only),
     * false for "> threshold" (referenceLow only). Never invents the
     * missing side of the range - a one-sided threshold stays
     * one-sided in the stored data (4.11), ResultValidator handles
     * status for a partial range explicitly rather than treating it
     * as UNKNOWN.
     */
    private ExtractedResult buildNumericThreshold(Matcher m, boolean isUpperBound) {
        String testName = m.group(1).trim();
        String rawValue = m.group(2);
        String unit = emptyToNull(m.group(3));
        Double threshold = parseNumber(m.group(4));
        Double value = parseNumber(rawValue);

        if (value == null || threshold == null) {
            return null;
        }

        String normalized = vocabulary.normalize(testName);
        double confidence = baseConfidence(0.85, normalized);

        Double low = isUpperBound ? null : threshold;
        Double high = isUpperBound ? threshold : null;

        return new ExtractedResult(testName, normalized, rawValue, value, null, unit, low, high, confidence);
    }

    private ExtractedResult buildNumericNoRange(Matcher m) {
        String testName = m.group(1).trim();
        String rawValue = m.group(2);
        String unit = emptyToNull(m.group(3));
        Double value = parseNumber(rawValue);

        if (value == null) {
            return null;
        }

        String normalized = vocabulary.normalize(testName);
        double confidence = baseConfidence(0.75, normalized);

        return new ExtractedResult(testName, normalized, rawValue, value, null, unit, null, null, confidence);
    }

    private ExtractedResult buildQualitative(Matcher m) {
        String testName = m.group(1).trim();
        String rawValue = m.group(2);

        String normalized = vocabulary.normalize(testName);
        double confidence = baseConfidence(0.85, normalized);

        return new ExtractedResult(testName, normalized, rawValue, null, rawValue, null, null, null, confidence);
    }

    private double baseConfidence(double base, String normalizedTestName) {
        double confidence = normalizedTestName != null ? base + 0.05 : base;
        return Math.min(confidence, 0.99);
    }

    private Double parseNumber(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Double.parseDouble(raw.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}