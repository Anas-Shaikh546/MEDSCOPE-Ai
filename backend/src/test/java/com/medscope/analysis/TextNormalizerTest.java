package com.medscope.analysis;

import com.medscope.analysis.extractor.TextNormalizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextNormalizerTest {

    private final TextNormalizer normalizer = new TextNormalizer();

    @Test
    void collapsesMultipleSpacesAndTrims() {
        List<String> lines = normalizer.toLines("Hemoglobin        13.8 g/dL       13.0 - 17.0");

        assertEquals(1, lines.size());
        assertEquals("Hemoglobin 13.8 g/dL 13.0 - 17.0", lines.get(0));
    }

    @Test
    void dropsBlankLines() {
        List<String> lines = normalizer.toLines("Hemoglobin 13.8 g/dL\n\n\nWBC Count 7,200 /uL");

        assertEquals(2, lines.size());
    }

    @Test
    void handlesNullInput() {
        assertTrue(normalizer.toLines(null).isEmpty());
    }
}