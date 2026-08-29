package com.medscope.analysis;

import com.medscope.analysis.extractor.ExtractedResult;
import com.medscope.analysis.extractor.MedicalResultExtractor;
import com.medscope.analysis.extractor.MedicalTestVocabulary;
import com.medscope.analysis.extractor.TextNormalizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MedicalResultExtractorTest {

    private final TextNormalizer normalizer = new TextNormalizer();
    private final MedicalResultExtractor extractor = new MedicalResultExtractor(new MedicalTestVocabulary());

    @Test
    void extractsNumericResultWithRangeAndRecognizedName() {
        List<String> lines = normalizer.toLines("Hemoglobin 13.8 g/dL 13.0 - 17.0");

        List<ExtractedResult> results = extractor.extract(lines);

        assertEquals(1, results.size());
        ExtractedResult r = results.get(0);
        assertEquals("Hemoglobin", r.testName());
        assertEquals("hemoglobin", r.normalizedTestName());
        assertEquals("13.8", r.rawValue());
        assertEquals(13.8, r.numericValue());
        assertNull(r.textValue());
        assertEquals("g/dL", r.unit());
        assertEquals(13.0, r.referenceLow());
        assertEquals(17.0, r.referenceHigh());
        assertTrue(r.confidence() >= 0.9);
    }

    @Test
    void extractsNumericResultWithThousandsSeparatorsInValueAndRange() {
        List<String> lines = normalizer.toLines("WBC Count 7,200 /uL 4,000 - 11,000");

        List<ExtractedResult> results = extractor.extract(lines);

        assertEquals(1, results.size());
        ExtractedResult r = results.get(0);
        assertEquals(7200.0, r.numericValue());
        assertEquals(4000.0, r.referenceLow());
        assertEquals(11000.0, r.referenceHigh());
    }

    @Test
    void extractsNumericResultWithoutRange() {
        List<String> lines = normalizer.toLines("Glucose 95 mg/dL");

        List<ExtractedResult> results = extractor.extract(lines);

        assertEquals(1, results.size());
        ExtractedResult r = results.get(0);
        assertEquals(95.0, r.numericValue());
        assertNull(r.referenceLow());
        assertNull(r.referenceHigh());
        // no-range pattern gets a lower base confidence than with-range
        assertTrue(r.confidence() < 0.9);
    }

    @Test
    void extractsQualitativeResult() {
        List<String> lines = normalizer.toLines("HIV Non-reactive");

        List<ExtractedResult> results = extractor.extract(lines);

        assertEquals(1, results.size());
        ExtractedResult r = results.get(0);
        assertNull(r.numericValue());
        assertEquals("Non-reactive", r.textValue());
        assertEquals("Non-reactive", r.rawValue());
    }

    @Test
    void unrecognizedTestNameGetsNullNormalizedName_notAGuess() {
        List<String> lines = normalizer.toLines("Vitamin D 32 ng/mL 30 - 100");

        List<ExtractedResult> results = extractor.extract(lines);

        assertEquals(1, results.size());
        assertNull(results.get(0).normalizedTestName());
    }

    @Test
    void unrelatedTextLinesProduceNoResults() {
        List<String> lines = normalizer.toLines("Patient: John Doe\nComplete Blood Count\nLab: Central Diagnostics");

        List<ExtractedResult> results = extractor.extract(lines);

        assertTrue(results.isEmpty());
    }

    @Test
    void fullReportExample_extractsAllThreeResults() {
        String reportText = String.join("\n",
                "Patient: John Doe",
                "Complete Blood Count",
                "Hemoglobin 13.8 g/dL 13.0 - 17.0",
                "WBC Count 7,200 /uL 4,000 - 11,000",
                "Platelets 250,000 /uL 150,000 - 450,000"
        );

        List<String> lines = normalizer.toLines(reportText);
        List<ExtractedResult> results = extractor.extract(lines);

        assertEquals(3, results.size());
        assertEquals("hemoglobin", results.get(0).normalizedTestName());
        assertEquals("wbc", results.get(1).normalizedTestName());
        assertEquals("platelets", results.get(2).normalizedTestName());
    }
}