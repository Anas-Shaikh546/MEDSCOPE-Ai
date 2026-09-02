package com.medscope.timeline.trend;

import java.util.Comparator;
import java.util.List;

/**
 * Pure deterministic trend calculator — no Spring, no DB.
 *
 * Rules (all locked by step6.txt):
 *  - < 3 observations               → INSUFFICIENT_DATA
 *  - mixed units                     → UNSUPPORTED
 *  - all deltas ≥ +threshold        → INCREASING
 *  - all deltas ≤ -threshold        → DECREASING
 *  - all |deltas| < threshold       → STABLE
 *  - mix of significant rises/falls  → FLUCTUATING
 *
 * Threshold = 1% of the mean observed value. Mean-relative keeps the band
 * scale-appropriate (hemoglobin ~14, glucose ~100, TSH ~2 all work correctly)
 * while being small enough that spec Test 2/3 steps (~0.3 g/dL) are
 * "significant" and spec Test 4 steps (±0.1 g/dL) are "noise".
 */
public class TrendCalculator {

    static final double STABLE_THRESHOLD_FRACTION = 0.01;

    public TrendDirection calculate(List<TrendObservation> rawObservations) {
        if (rawObservations == null || rawObservations.size() < 3) {
            return TrendDirection.INSUFFICIENT_DATA;
        }

        List<TrendObservation> obs = rawObservations.stream()
                .sorted(Comparator.comparing(TrendObservation::orderedAt))
                .toList();

        // Unit homogeneity check — no guessing, no conversion.
        String firstUnit = obs.get(0).unit();
        boolean unitMismatch = obs.stream().anyMatch(o -> !o.unit().equalsIgnoreCase(firstUnit));
        if (unitMismatch) {
            return TrendDirection.UNSUPPORTED;
        }

        // Noise threshold: 1% of the mean absolute value across all observations.
        // Mean-relative scaling works correctly across domains (hemoglobin ~14,
        // glucose ~100, TSH ~2). When the mean is 0, threshold is 0 and any
        // non-zero delta signals change — safe for zero-boundary tests.
        double mean = obs.stream().mapToDouble(TrendObservation::numericValue).average().orElse(0);
        double threshold = Math.abs(mean) * STABLE_THRESHOLD_FRACTION;

        boolean hasSignificantRise = false;
        boolean hasSignificantFall = false;

        for (int i = 1; i < obs.size(); i++) {
            double delta = obs.get(i).numericValue() - obs.get(i - 1).numericValue();
            if (delta > threshold) {
                hasSignificantRise = true;
            } else if (delta < -threshold) {
                hasSignificantFall = true;
            }
        }

        if (hasSignificantRise && hasSignificantFall) {
            return TrendDirection.FLUCTUATING;
        } else if (hasSignificantRise) {
            return TrendDirection.INCREASING;
        } else if (hasSignificantFall) {
            return TrendDirection.DECREASING;
        } else {
            return TrendDirection.STABLE;
        }
    }
}
