package com.medscope.analysis.extractor;

import com.medscope.ocr.model.OcrMetadata;
import com.medscope.ocr.orchestrator.OcrOrchestrator;
import com.medscope.ocr.orchestrator.OcrProcessingResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfTextExtractorTest {

    @Mock
    private OcrOrchestrator ocrOrchestrator;

    @InjectMocks
    private PdfTextExtractor extractor;

    @Test
    void textBasedPdf_isSupportedAndTextIsExtracted() throws IOException {
        byte[] pdfBytes = buildTextPdf("Hemoglobin 13.8 g/dL 13.0 - 17.0");

        ExtractedText result = extractor.extract(pdfBytes);

        assertTrue(result.supported());
        assertTrue(result.rawText().contains("Hemoglobin"));
        assertEquals(1, result.pageCount());
    }

    @Test
    void blankPagePdf_isUnsupported() throws IOException {
        byte[] pdfBytes = buildBlankPdf();
        when(ocrOrchestrator.process(any())).thenReturn(
            OcrProcessingResult.builder()
                .extractedText("")
                .pageCount(1)
                .metadata(OcrMetadata.builder()
                    .ocrUsed(true)
                    .averageConfidence(0.0)
                    .ocrPages(List.of(0))
                    .processedAt(java.time.Instant.now())
                    .attempts(1)
                    .preprocessingApplied(false)
                    .build())
                .build()
        );

        ExtractedText result = extractor.extract(pdfBytes);

        assertFalse(result.supported());
    }

    @Test
    void garbageBytes_areHandledAsUnsupportedNotAnException() {
        byte[] garbage = "not a real pdf".getBytes();

        ExtractedText result = extractor.extract(garbage);

        assertFalse(result.supported());
    }

    private byte[] buildTextPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildBlankPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}