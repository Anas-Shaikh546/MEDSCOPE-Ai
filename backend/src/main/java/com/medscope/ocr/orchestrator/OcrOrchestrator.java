package com.medscope.ocr.orchestrator;

import com.medscope.ocr.classifier.PdfPageClassifier;
import com.medscope.ocr.model.OcrMetadata;
import com.medscope.ocr.model.PageAnalysis;
import com.medscope.ocr.model.PageType;
import com.medscope.ocr.preprocessing.ImagePreprocessor;
import com.medscope.ocr.service.OcrResult;
import com.medscope.ocr.service.OcrService;
import com.medscope.ocr.validation.MedicalOcrValidator;
import com.medscope.ocr.validation.OcrValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the complete OCR pipeline for PDF documents.
 *
 * Flow (from step8.txt section 3):
 * 1. Classify each page (TEXT/SCANNED/MIXED)
 * 2. Extract text directly from TEXT pages
 * 3. Apply OCR to SCANNED pages
 * 4. Combine results preserving page numbers
 * 5. Return normalized text ready for existing extraction pipeline
 *
 * Key principle: OCR should only change how text is obtained, not the
 * extraction logic itself.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OcrOrchestrator {

    private final PdfPageClassifier pageClassifier;
    private final OcrService ocrService;
    private final ImagePreprocessor preprocessor;
    private final MedicalOcrValidator validator;

    // OCR confidence threshold for retry with preprocessing
    private static final double LOW_CONFIDENCE_THRESHOLD = 0.75;

    // DPI for rendering PDF pages to images
    private static final int RENDER_DPI = 300;

    /**
     * Process a PDF document, applying OCR only where necessary.
     *
     * @param pdfBytes PDF file bytes
     * @return Combined extracted text and OCR metadata
     */
    public OcrProcessingResult process(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int pageCount = document.getNumberOfPages();

            if (pageCount == 0) {
                return OcrProcessingResult.builder()
                    .extractedText("")
                    .pageCount(0)
                    .metadata(OcrMetadata.builder()
                        .ocrUsed(false)
                        .ocrPages(List.of())
                        .averageConfidence(0.0)
                        .processedAt(Instant.now())
                        .attempts(0)
                        .preprocessingApplied(false)
                        .build())
                    .build();
            }

            log.info("Processing PDF with {} pages", pageCount);

            // Step 1: Classify all pages
            List<PageAnalysis> pageAnalyses = classifyAllPages(document);

            // Step 2: Process each page based on classification
            StringBuilder combinedText = new StringBuilder();
            List<Integer> ocrPages = new ArrayList<>();
            List<Double> confidences = new ArrayList<>();
            boolean anyPreprocessing = false;
            int totalAttempts = 0;

            PDFTextStripper textStripper = new PDFTextStripper();
            PDFRenderer renderer = new PDFRenderer(document);

            for (int i = 0; i < pageCount; i++) {
                PageAnalysis analysis = pageAnalyses.get(i);
                combinedText.append("\n--- Page ").append(i + 1).append(" ---\n");

                if (analysis.getPageType() == PageType.TEXT) {
                    // Extract text directly (no OCR needed)
                    log.debug("Page {} is TEXT type, extracting directly", i);
                    textStripper.setStartPage(i + 1);
                    textStripper.setEndPage(i + 1);
                    String pageText = textStripper.getText(document);
                    combinedText.append(pageText);
                    confidences.add(1.0); // Digital text is 100% confident

                } else if (analysis.getPageType() == PageType.SCANNED ||
                           analysis.getPageType() == PageType.MIXED) {
                    // Apply OCR
                    log.info("Page {} is {} type, applying OCR", i, analysis.getPageType());
                    ocrPages.add(i);

                    OcrPageResult ocrResult = processPageWithOcr(renderer, i);
                    combinedText.append(ocrResult.text);
                    confidences.add(ocrResult.confidence);
                    totalAttempts += ocrResult.attempts;
                    if (ocrResult.preprocessed) {
                        anyPreprocessing = true;
                    }

                } else {
                    // EMPTY page
                    log.debug("Page {} is EMPTY, skipping", i);
                    confidences.add(1.0);
                }
            }

            // Calculate average confidence
            double avgConfidence = confidences.isEmpty() ? 0.0 :
                confidences.stream().mapToDouble(d -> d).average().orElse(0.0);

            // Build metadata
            OcrMetadata metadata = OcrMetadata.builder()
                .ocrUsed(!ocrPages.isEmpty())
                .ocrEngine(ocrService.getEngineName())
                .ocrEngineVersion(ocrService.getEngineVersion())
                .ocrPages(ocrPages)
                .averageConfidence(avgConfidence)
                .processedAt(Instant.now())
                .attempts(totalAttempts)
                .preprocessingApplied(anyPreprocessing)
                .build();

            log.info("PDF processing complete: {} pages, {} OCR'd, avg confidence: {:.2f}",
                pageCount, ocrPages.size(), avgConfidence);

            return OcrProcessingResult.builder()
                .extractedText(combinedText.toString())
                .pageCount(pageCount)
                .metadata(metadata)
                .pageAnalyses(pageAnalyses)
                .build();
        }
    }

    /**
     * Classify all pages in the document.
     */
    private List<PageAnalysis> classifyAllPages(PDDocument document) throws IOException {
        int pageCount = document.getNumberOfPages();
        List<PageAnalysis> analyses = new ArrayList<>(pageCount);

        for (int i = 0; i < pageCount; i++) {
            PageAnalysis analysis = pageClassifier.analyzePage(document, i);
            analyses.add(analysis);
            log.debug("Page {} classified as {}: textLen={}, imgRatio={:.2f}, confidence={:.2f}",
                i, analysis.getPageType(), analysis.getTextLength(),
                analysis.getImageAreaRatio(), analysis.getConfidence());
        }

        return analyses;
    }

    /**
     * Process a single page with OCR, applying preprocessing if needed.
     */
    private OcrPageResult processPageWithOcr(PDFRenderer renderer, int pageIndex) throws IOException {
        // Render page to image
        BufferedImage image = renderer.renderImageWithDPI(pageIndex, RENDER_DPI);

        // First attempt: minimal preprocessing
        BufferedImage preprocessed = preprocessor.preprocess(image, false);
        OcrResult ocrResult = ocrService.processPage(preprocessed, pageIndex);

        int attempts = 1;
        boolean usedAggressive = false;

        // Retry with aggressive preprocessing if confidence is low
        if (ocrResult.getConfidence() < LOW_CONFIDENCE_THRESHOLD) {
            log.info("Page {} OCR confidence low ({:.2f}), retrying with aggressive preprocessing",
                pageIndex, ocrResult.getConfidence());

            preprocessed = preprocessor.preprocess(image, true);
            OcrResult retryResult = ocrService.processPage(preprocessed, pageIndex);
            attempts++;
            usedAggressive = true;

            // Use retry result if it's better
            if (retryResult.getConfidence() > ocrResult.getConfidence()) {
                ocrResult = retryResult;
                log.info("Retry improved confidence: {:.2f} -> {:.2f}",
                    ocrResult.getConfidence(), retryResult.getConfidence());
            }
        }

        return new OcrPageResult(
            ocrResult.getRawText(),
            ocrResult.getConfidence(),
            attempts,
            usedAggressive
        );
    }

    /**
     * Internal result for OCR processing of a single page.
     */
    private static class OcrPageResult {
        final String text;
        final double confidence;
        final int attempts;
        final boolean preprocessed;

        OcrPageResult(String text, double confidence, int attempts, boolean preprocessed) {
            this.text = text;
            this.confidence = confidence;
            this.attempts = attempts;
            this.preprocessed = preprocessed;
        }
    }
}
