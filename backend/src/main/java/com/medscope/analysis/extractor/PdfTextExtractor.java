package com.medscope.analysis.extractor;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * PDFBox wrapper - the only place in the codebase that touches PDF
 * internals (4.3: "don't write your own PDF parser", use a real library).
 *
 * Deliberately dumb about *content*: it extracts raw text and decides
 * supported vs unsupported based on whether there's any meaningful text
 * at all. It does not know what a "medical result" looks like - that's
 * MedicalResultExtractor's job (Task 3), kept separate on purpose.
 */
@Component
@Slf4j
public class PdfTextExtractor {

    // Below this average number of characters per page, we treat the
    // PDF as having no usable text layer (most likely a scanned image)
    // rather than a text-based PDF that just happens to be sparse.
    private static final int MIN_AVG_CHARS_PER_PAGE = 20;

    public ExtractedText extract(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int pageCount = document.getNumberOfPages();

            if (pageCount == 0) {
                return ExtractedText.unsupported(0);
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);

            if (!hasUsableText(rawText, pageCount)) {
                log.info("PDF has no usable text layer, treating as unsupported: pageCount={}", pageCount);
                return ExtractedText.unsupported(pageCount);
            }

            return ExtractedText.of(rawText, pageCount);

        } catch (IOException e) {
            // A PDF that fails to even load is unsupported, not a hard
            // failure - the report was already validated as a real PDF
            // at upload time (magic bytes), so this is a "can't extract
            // from this particular structure" case, not corruption we
            // need to surface as FAILED.
            log.warn("PDF failed to load for text extraction", e);
            return ExtractedText.unsupported(0);
        }
    }

    private boolean hasUsableText(String rawText, int pageCount) {
        if (rawText == null) {
            return false;
        }
        String trimmed = rawText.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        double avgCharsPerPage = (double) trimmed.length() / pageCount;
        return avgCharsPerPage >= MIN_AVG_CHARS_PER_PAGE;
    }
}