package com.medscope.ocr.validation;

public enum OcrConfidenceLevel {
    HIGH,    // > 0.90 - Accept for medical data
    MEDIUM,  // 0.75 - 0.90 - Flag for review
    LOW      // < 0.75 - Reject
}
