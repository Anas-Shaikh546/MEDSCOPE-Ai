package com.medscope.ocr.classifier;

import com.medscope.ocr.model.PageAnalysis;
import com.medscope.ocr.model.PageType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Multi-signal PDF page classifier that determines whether a page contains
 * digital text, is a scanned image, or is mixed content.
 *
 * Uses four signals as recommended in step8.txt section 4:
 * - Signal A: Extracted text length
 * - Signal B: Text density (characters per page area)
 * - Signal C: Image presence and coverage
 * - Signal D: Text quality (garbled detection)
 */
@Component
@Slf4j
public class PdfPageClassifier {

    // Thresholds for classification
    private static final int MIN_TEXT_LENGTH_FOR_DIGITAL = 50;
    private static final int MAX_TEXT_LENGTH_FOR_SCANNED = 10;
    private static final double MIN_TEXT_DENSITY_FOR_DIGITAL = 0.5; // chars per square inch
    private static final double IMAGE_AREA_THRESHOLD_FOR_SCANNED = 0.7; // 70% of page
    private static final double MIN_IMAGE_SIZE_FOR_SCAN = 500_000; // pixels (approx 700x700)
    private static final double GARBLED_TEXT_THRESHOLD = 0.3; // 30% non-printable/weird chars

    // Pattern to detect garbled text (excessive special chars, control chars, etc)
    private static final Pattern GARBLED_PATTERN = Pattern.compile("[^\\p{Print}\\s]");
    private static final Pattern MOSTLY_DIGITS_SPECIAL = Pattern.compile("[^a-zA-Z]{10,}");

    /**
     * Analyze a single page to determine its type.
     */
    public PageAnalysis analyzePage(PDDocument document, int pageIndex) throws IOException {
        PDPage page = document.getPage(pageIndex);

        // Signal A: Extract text length
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageIndex + 1);
        stripper.setEndPage(pageIndex + 1);
        String pageText = stripper.getText(document);
        int textLength = pageText.trim().length();

        // Signal B: Calculate text density
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        double pageArea = (pageWidth * pageHeight) / 144.0; // rough conversion to square inches
        double textDensity = pageArea > 0 ? textLength / pageArea : 0;

        // Signal C: Check for large images
        ImageAnalysisResult imageAnalysis = analyzeImages(page, pageWidth, pageHeight);

        // Signal D: Check text quality
        boolean textIsGarbled = isTextGarbled(pageText);

        // Classify based on combined signals
        PageType pageType = classifyPage(
            textLength,
            textDensity,
            imageAnalysis.hasLargeImages,
            imageAnalysis.imageAreaRatio,
            textIsGarbled
        );

        // Calculate confidence
        double confidence = calculateConfidence(
            textLength,
            textDensity,
            imageAnalysis.imageAreaRatio,
            textIsGarbled,
            pageType
        );

        return PageAnalysis.builder()
            .pageIndex(pageIndex)
            .pageType(pageType)
            .textLength(textLength)
            .textDensity(textDensity)
            .hasLargeImages(imageAnalysis.hasLargeImages)
            .imageAreaRatio(imageAnalysis.imageAreaRatio)
            .textIsGarbled(textIsGarbled)
            .confidence(confidence)
            .build();
    }

    /**
     * Classify page based on multiple signals.
     * Logic follows step8.txt section 4 recommendations.
     */
    private PageType classifyPage(
        int textLength,
        double textDensity,
        boolean hasLargeImages,
        double imageAreaRatio,
        boolean textIsGarbled
    ) {
        // EMPTY: Almost no content
        if (textLength < 5 && !hasLargeImages) {
            return PageType.EMPTY;
        }

        // SCANNED: High image coverage + minimal/garbled text
        if (imageAreaRatio > IMAGE_AREA_THRESHOLD_FOR_SCANNED
            && (textLength < MAX_TEXT_LENGTH_FOR_SCANNED || textIsGarbled)) {
            log.debug("Classified as SCANNED: imageAreaRatio={}, textLength={}, garbled={}",
                imageAreaRatio, textLength, textIsGarbled);
            return PageType.SCANNED;
        }

        // TEXT: Sufficient text, low image coverage, good text quality
        if (textLength >= MIN_TEXT_LENGTH_FOR_DIGITAL
            && textDensity >= MIN_TEXT_DENSITY_FOR_DIGITAL
            && !textIsGarbled
            && imageAreaRatio < 0.5) {
            return PageType.TEXT;
        }

        // SCANNED: Minimal usable text regardless of images
        if (textLength < MAX_TEXT_LENGTH_FOR_SCANNED || textIsGarbled) {
            return PageType.SCANNED;
        }

        // MIXED: Has both substantial text and images
        if (hasLargeImages && textLength >= MIN_TEXT_LENGTH_FOR_DIGITAL) {
            return PageType.MIXED;
        }

        // Default to TEXT if we have reasonable text content
        return textLength >= MIN_TEXT_LENGTH_FOR_DIGITAL ? PageType.TEXT : PageType.SCANNED;
    }

    /**
     * Analyze images on the page to detect scanned content.
     */
    private ImageAnalysisResult analyzeImages(PDPage page, float pageWidth, float pageHeight) throws IOException {
        PDResources resources = page.getResources();
        if (resources == null) {
            return new ImageAnalysisResult(false, 0.0);
        }

        boolean hasLargeImages = false;
        double totalImageArea = 0.0;
        double pageArea = pageWidth * pageHeight;

        try {
            for (var name : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(name);

                if (xObject instanceof PDImageXObject) {
                    PDImageXObject image = (PDImageXObject) xObject;
                    int width = image.getWidth();
                    int height = image.getHeight();
                    long pixels = (long) width * height;

                    // Check if this is a large image (potential scan)
                    if (pixels >= MIN_IMAGE_SIZE_FOR_SCAN) {
                        hasLargeImages = true;

                        // Estimate image coverage (assume image fills similar area on page)
                        double imageArea = width * height;
                        totalImageArea += imageArea;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error analyzing page images, assuming no large images", e);
            return new ImageAnalysisResult(false, 0.0);
        }

        // Calculate rough image area ratio
        // This is approximate since we don't have exact rendering positions
        double imageAreaRatio = Math.min(1.0, totalImageArea / (pageArea * 5)); // Scale factor adjustment

        return new ImageAnalysisResult(hasLargeImages, imageAreaRatio);
    }

    /**
     * Check if extracted text appears garbled or corrupted.
     * Garbled text suggests the PDF has a text layer but it's not useful.
     */
    private boolean isTextGarbled(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false; // Empty is not garbled, just empty
        }

        String trimmed = text.trim();

        // Count non-printable characters
        int garbageCount = GARBLED_PATTERN.matcher(trimmed).results().mapToInt(m -> m.group().length()).sum();
        double garbageRatio = (double) garbageCount / trimmed.length();

        if (garbageRatio > GARBLED_TEXT_THRESHOLD) {
            return true;
        }

        // Check for extremely fragmented text (common in bad OCR/corrupted PDFs)
        // e.g., "H e m o g l o b i n" or random character sequences
        String[] words = trimmed.split("\\s+");
        int singleCharWords = 0;
        for (String word : words) {
            if (word.length() == 1 && !word.matches("[aAI]")) { // Ignore valid single-char words
                singleCharWords++;
            }
        }

        if (words.length > 10 && singleCharWords > words.length * 0.5) {
            return true; // More than 50% single-char "words" suggests garbled text
        }

        return false;
    }

    /**
     * Calculate confidence in the classification.
     * Higher confidence = clearer signals.
     */
    private double calculateConfidence(
        int textLength,
        double textDensity,
        double imageAreaRatio,
        boolean textIsGarbled,
        PageType pageType
    ) {
        double confidence = 0.5; // Base confidence

        switch (pageType) {
            case TEXT:
                // Strong text signals increase confidence
                if (textLength > 200) confidence += 0.2;
                if (textDensity > 2.0) confidence += 0.15;
                if (imageAreaRatio < 0.1) confidence += 0.15;
                break;

            case SCANNED:
                // Strong scanned signals increase confidence
                if (imageAreaRatio > 0.8) confidence += 0.25;
                if (textLength < 5) confidence += 0.15;
                if (textIsGarbled) confidence += 0.1;
                break;

            case MIXED:
                // Mixed is inherently less confident
                confidence = 0.6;
                break;

            case EMPTY:
                // Empty is usually clear
                if (textLength == 0 && imageAreaRatio < 0.01) {
                    confidence = 0.9;
                }
                break;
        }

        return Math.min(1.0, confidence);
    }

    private static class ImageAnalysisResult {
        final boolean hasLargeImages;
        final double imageAreaRatio;

        ImageAnalysisResult(boolean hasLargeImages, double imageAreaRatio) {
            this.hasLargeImages = hasLargeImages;
            this.imageAreaRatio = imageAreaRatio;
        }
    }
}
