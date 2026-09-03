package com.medscope.ocr.service;

import lombok.Value;

/**
 * Bounding box for OCR text regions.
 */
@Value
public class BoundingBox {
    int x;
    int y;
    int width;
    int height;
}
