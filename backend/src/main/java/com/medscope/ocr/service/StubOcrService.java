package com.medscope.ocr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Stub OCR service implementation. Replace with actual PaddleOCR or Tesseract
 * implementation when ready to use OCR functionality.
 *
 * This stub always returns empty results to allow the application to compile
 * and existing tests to pass.
 */
@Component
@Slf4j
public class StubOcrService implements OcrService {

    @Override
    public OcrResult processPage(BufferedImage image, int pageNumber) throws IOException {
        log.warn("StubOcrService is being used - OCR functionality not yet implemented");

        return OcrResult.builder()
            .pageNumber(pageNumber)
            .rawText("")
            .confidence(0.0)
            .processingTimeMs(0)
            .words(new ArrayList<>())
            .preprocessed(false)
            .engine("stub")
            .engineVersion("0.0.0")
            .build();
    }

    @Override
    public String getEngineName() {
        return "StubOCR";
    }

    @Override
    public String getEngineVersion() {
        return "0.0.0-stub";
    }
}
