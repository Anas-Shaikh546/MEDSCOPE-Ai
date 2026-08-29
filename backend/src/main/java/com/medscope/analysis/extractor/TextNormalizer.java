package com.medscope.analysis.extractor;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Cleans PDFBox's raw text output into individual lines that
 * MedicalResultExtractor can reason about. Deliberately dumb: this
 * class knows nothing about medical tests, only about turning messy
 * whitespace/line-break noise into a clean list of lines.
 */
@Component
public class TextNormalizer {

    public List<String> toLines(String rawText) {
        List<String> lines = new ArrayList<>();
        if (rawText == null) {
            return lines;
        }

        for (String line : rawText.split("\\r?\\n")) {
            String cleaned = collapseWhitespace(line);
            if (!cleaned.isEmpty()) {
                lines.add(cleaned);
            }
        }
        return lines;
    }

    private String collapseWhitespace(String line) {
        // PDFBox frequently emits runs of spaces/tabs where a PDF used
        // visual column alignment - collapse to single spaces so a
        // fixed-pattern regex downstream doesn't have to account for
        // arbitrary whitespace widths.
        return line.replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" {2,}", " ")
                .trim();
    }
}