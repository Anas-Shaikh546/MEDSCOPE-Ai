package com.medscope.timeline.trend;

import java.time.Instant;

/**
 * A single data point passed to TrendCalculator.
 * orderedAt is used only for sorting (test_date → createdAt fallback).
 */
public record TrendObservation(double numericValue, String unit, Instant orderedAt) {

    public TrendObservation {
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("unit must not be blank");
        }
        if (orderedAt == null) {
            throw new IllegalArgumentException("orderedAt must not be null");
        }
    }
}
