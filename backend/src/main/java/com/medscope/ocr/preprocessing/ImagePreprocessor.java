package com.medscope.ocr.preprocessing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Image preprocessing pipeline for OCR quality improvement.
 *
 * Applies adaptive preprocessing based on image quality:
 * - Rotation correction
 * - Grayscale conversion
 * - Noise reduction
 * - Contrast normalization
 * - Deskew
 * - Margin cropping
 *
 * Philosophy from step8.txt section 7:
 * "Don't aggressively preprocess every document. Some medical reports
 * already have excellent scan quality. Only preprocess if initial OCR
 * confidence is low."
 */
@Component
@Slf4j
public class ImagePreprocessor {

    /**
     * Apply preprocessing to improve OCR accuracy.
     *
     * @param original Original scanned image
     * @param applyAggressive Whether to apply aggressive preprocessing
     *                        (true if initial OCR confidence was low)
     * @return Preprocessed image ready for OCR
     */
    public BufferedImage preprocess(BufferedImage original, boolean applyAggressive) {
        if (original == null) {
            throw new IllegalArgumentException("Original image cannot be null");
        }

        BufferedImage processed = original;

        // Always apply: basic grayscale conversion for better OCR
        processed = toGrayscale(processed);

        if (applyAggressive) {
            log.debug("Applying aggressive preprocessing");

            // Detect and correct orientation
            processed = correctOrientation(processed);

            // Noise reduction
            processed = reduceNoise(processed);

            // Contrast enhancement
            processed = normalizeContrast(processed);

            // Deskew (straighten tilted scans)
            processed = deskew(processed);

            // Crop unnecessary margins
            processed = cropMargins(processed);
        } else {
            log.debug("Applying minimal preprocessing (good quality scan)");

            // Light preprocessing only
            processed = normalizeContrast(processed);
        }

        return processed;
    }

    /**
     * Convert image to grayscale for better OCR accuracy.
     */
    private BufferedImage toGrayscale(BufferedImage original) {
        if (original.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            return original; // Already grayscale
        }

        BufferedImage grayscale = new BufferedImage(
            original.getWidth(),
            original.getHeight(),
            BufferedImage.TYPE_BYTE_GRAY
        );

        Graphics2D g = grayscale.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        return grayscale;
    }

    /**
     * Detect and correct image orientation (90°, 180°, 270° rotations).
     * Medical reports are sometimes scanned in wrong orientation.
     */
    private BufferedImage correctOrientation(BufferedImage image) {
        // TODO: Implement orientation detection
        // Could use: edge detection, text line detection, or ML-based detection
        // For now, return as-is (orientation detection is complex)
        log.debug("Orientation detection not yet implemented");
        return image;
    }

    /**
     * Reduce noise using median filter or bilateral filter.
     * Helps with low-quality scans and compression artifacts.
     */
    private BufferedImage reduceNoise(BufferedImage image) {
        // Simple 3x3 median filter to reduce noise
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage denoised = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        int[] window = new int[9];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                // Collect 3x3 neighborhood
                int idx = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int rgb = image.getRGB(x + dx, y + dy);
                        window[idx++] = rgb & 0xFF; // Get grayscale value
                    }
                }

                // Sort and take median
                java.util.Arrays.sort(window);
                int median = window[4];

                // Set pixel
                int gray = (median << 16) | (median << 8) | median;
                denoised.setRGB(x, y, gray);
            }
        }

        // Copy borders
        for (int y = 0; y < height; y++) {
            denoised.setRGB(0, y, image.getRGB(0, y));
            denoised.setRGB(width - 1, y, image.getRGB(width - 1, y));
        }
        for (int x = 0; x < width; x++) {
            denoised.setRGB(x, 0, image.getRGB(x, 0));
            denoised.setRGB(x, height - 1, image.getRGB(x, height - 1));
        }

        return denoised;
    }

    /**
     * Normalize contrast using histogram equalization.
     * Improves readability of faded or low-contrast scans.
     */
    private BufferedImage normalizeContrast(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Calculate histogram
        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int gray = rgb & 0xFF;
                histogram[gray]++;
            }
        }

        // Calculate cumulative distribution
        int[] cdf = new int[256];
        cdf[0] = histogram[0];
        for (int i = 1; i < 256; i++) {
            cdf[i] = cdf[i - 1] + histogram[i];
        }

        // Normalize CDF
        int total = width * height;
        int[] equalized = new int[256];
        for (int i = 0; i < 256; i++) {
            equalized[i] = (int) ((cdf[i] * 255.0) / total);
        }

        // Apply equalization
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int gray = rgb & 0xFF;
                int newGray = equalized[gray];
                int newRgb = (newGray << 16) | (newGray << 8) | newGray;
                result.setRGB(x, y, newRgb);
            }
        }

        return result;
    }

    /**
     * Deskew image (correct tilt/rotation).
     * Medical reports are sometimes scanned at slight angles.
     */
    private BufferedImage deskew(BufferedImage image) {
        // TODO: Implement deskew using Hough transform or projection profile
        // This is complex and requires angle detection
        log.debug("Deskewing not yet implemented");
        return image;
    }

    /**
     * Crop unnecessary margins (often present in scanned documents).
     * Reduces processing time and focuses OCR on content area.
     */
    private BufferedImage cropMargins(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Find content boundaries by detecting non-white pixels
        int minX = width;
        int maxX = 0;
        int minY = height;
        int maxY = 0;

        int whiteThreshold = 240; // Pixels brighter than this are considered "margin"

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int gray = rgb & 0xFF;

                if (gray < whiteThreshold) { // Found content
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        // Add small padding
        int padding = 10;
        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(width - 1, maxX + padding);
        maxY = Math.min(height - 1, maxY + padding);

        // If no content found or margins are tiny, return original
        if (maxX <= minX || maxY <= minY ||
            (maxX - minX > width * 0.95 && maxY - minY > height * 0.95)) {
            return image;
        }

        // Crop to content area
        return image.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /**
     * Estimate image quality to decide if aggressive preprocessing is needed.
     *
     * @return Quality score 0.0 (poor) to 1.0 (excellent)
     */
    public double estimateQuality(BufferedImage image) {
        // Simple quality estimation based on contrast and sharpness
        int width = image.getWidth();
        int height = image.getHeight();

        // Calculate contrast (standard deviation of pixel values)
        double mean = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int gray = rgb & 0xFF;
                mean += gray;
            }
        }
        mean /= (width * height);

        double variance = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int gray = rgb & 0xFF;
                variance += Math.pow(gray - mean, 2);
            }
        }
        variance /= (width * height);
        double contrast = Math.sqrt(variance);

        // Normalize contrast to 0-1 range
        // Good medical report scans typically have contrast around 40-60
        double qualityScore = Math.min(1.0, contrast / 60.0);

        return qualityScore;
    }
}
