package com.medscope.timeline.trend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers all pure-logic scenarios from step6.txt §6.21.
 * Tests 7-10 (different users, deleted reports, reference ranges, missing dates)
 * require the repository/service layer and live in TimelineServiceTest (Task 3).
 */
class TrendCalculatorTest {

    private TrendCalculator calculator;

    // Stable base timestamps — ordering is what matters, not wall-clock accuracy.
    private static final Instant T1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant T3 = Instant.parse("2026-03-01T00:00:00Z");
    private static final Instant T4 = Instant.parse("2026-04-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        calculator = new TrendCalculator();
    }

    // --- spec scenario 1 ---

    @Test
    void nullObservations_returnsInsufficientData() {
        assertEquals(TrendDirection.INSUFFICIENT_DATA, calculator.calculate(null));
    }

    @Test
    void emptyList_returnsInsufficientData() {
        assertEquals(TrendDirection.INSUFFICIENT_DATA, calculator.calculate(Collections.emptyList()));
    }

    @Test
    void oneObservation_returnsInsufficientData() {
        // step6.txt Test 1: Hemoglobin = 13.8 (single result)
        List<TrendObservation> obs = List.of(
                new TrendObservation(13.8, "g/dL", T1)
        );
        assertEquals(TrendDirection.INSUFFICIENT_DATA, calculator.calculate(obs));
    }

    @Test
    void twoObservations_returnsInsufficientData() {
        List<TrendObservation> obs = List.of(
                new TrendObservation(13.8, "g/dL", T1),
                new TrendObservation(14.0, "g/dL", T2)
        );
        assertEquals(TrendDirection.INSUFFICIENT_DATA, calculator.calculate(obs));
    }

    // --- spec scenario 2 ---

    @Test
    void threeIncreasing_returnsIncreasing() {
        // step6.txt Test 2: 13.1 → 13.4 → 13.8
        List<TrendObservation> obs = List.of(
                new TrendObservation(13.1, "g/dL", T1),
                new TrendObservation(13.4, "g/dL", T2),
                new TrendObservation(13.8, "g/dL", T3)
        );
        assertEquals(TrendDirection.INCREASING, calculator.calculate(obs));
    }

    @Test
    void fourIncreasing_returnsIncreasing() {
        List<TrendObservation> obs = List.of(
                new TrendObservation(13.1, "g/dL", T1),
                new TrendObservation(13.4, "g/dL", T2),
                new TrendObservation(13.8, "g/dL", T3),
                new TrendObservation(14.2, "g/dL", T4)
        );
        assertEquals(TrendDirection.INCREASING, calculator.calculate(obs));
    }

    // --- spec scenario 3 ---

    @Test
    void threeDecreasing_returnsDecreasing() {
        // step6.txt Test 3: 14.0 → 13.7 → 13.2
        List<TrendObservation> obs = List.of(
                new TrendObservation(14.0, "g/dL", T1),
                new TrendObservation(13.7, "g/dL", T2),
                new TrendObservation(13.2, "g/dL", T3)
        );
        assertEquals(TrendDirection.DECREASING, calculator.calculate(obs));
    }

    // --- spec scenario 4 ---

    @Test
    void threeStable_returnsStable() {
        // step6.txt Test 4: 13.7 → 13.8 → 13.7
        List<TrendObservation> obs = List.of(
                new TrendObservation(13.7, "g/dL", T1),
                new TrendObservation(13.8, "g/dL", T2),
                new TrendObservation(13.7, "g/dL", T3)
        );
        assertEquals(TrendDirection.STABLE, calculator.calculate(obs));
    }

    @Test
    void identicalValues_returnsStable() {
        List<TrendObservation> obs = List.of(
                new TrendObservation(5.0, "mmol/L", T1),
                new TrendObservation(5.0, "mmol/L", T2),
                new TrendObservation(5.0, "mmol/L", T3)
        );
        assertEquals(TrendDirection.STABLE, calculator.calculate(obs));
    }

    // --- spec scenario 5 ---

    @Test
    void mixedRisesAndFalls_returnsFluctuating() {
        // step6.txt Test 5: 13.2 → 14.0 → 13.1 → 14.1
        List<TrendObservation> obs = List.of(
                new TrendObservation(13.2, "g/dL", T1),
                new TrendObservation(14.0, "g/dL", T2),
                new TrendObservation(13.1, "g/dL", T3),
                new TrendObservation(14.1, "g/dL", T4)
        );
        assertEquals(TrendDirection.FLUCTUATING, calculator.calculate(obs));
    }

    // --- spec scenario 6 ---

    @Test
    void differentUnits_returnsUnsupported() {
        // step6.txt Test 6: unit mismatch must not produce a fake comparison
        List<TrendObservation> obs = List.of(
                new TrendObservation(13.8, "g/dL", T1),
                new TrendObservation(138.0, "g/L", T2),
                new TrendObservation(14.0, "g/dL", T3)
        );
        assertEquals(TrendDirection.UNSUPPORTED, calculator.calculate(obs));
    }

    @Test
    void differentUnitsAllThree_returnsUnsupported() {
        List<TrendObservation> obs = List.of(
                new TrendObservation(5.0, "mmol/L", T1),
                new TrendObservation(90.0, "mg/dL", T2),
                new TrendObservation(5.2, "mmol/L", T3)
        );
        assertEquals(TrendDirection.UNSUPPORTED, calculator.calculate(obs));
    }

    // --- unit comparison is case-insensitive ---

    @Test
    void sameUnitDifferentCase_isNotUnsupported() {
        List<TrendObservation> obs = List.of(
                new TrendObservation(13.1, "g/dL", T1),
                new TrendObservation(13.4, "G/DL", T2),
                new TrendObservation(13.8, "g/dl", T3)
        );
        // Units are same modulo case — no UNSUPPORTED. Trend is INCREASING.
        assertEquals(TrendDirection.INCREASING, calculator.calculate(obs));
    }

    // --- ordering by orderedAt, not insertion order ---

    @Test
    void unsortedInput_isSortedBeforeCalculation() {
        // Supplied in reverse — calculator must sort by orderedAt before evaluating.
        // T3(13.8) → T2(13.4) → T1(13.1): sorted order is increasing.
        List<TrendObservation> obs = List.of(
                new TrendObservation(13.8, "g/dL", T3),
                new TrendObservation(13.1, "g/dL", T1),
                new TrendObservation(13.4, "g/dL", T2)
        );
        assertEquals(TrendDirection.INCREASING, calculator.calculate(obs));
    }

    // --- TrendObservation validation ---

    @Test
    void blankUnit_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new TrendObservation(13.8, "  ", T1));
    }

    @Test
    void nullUnit_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new TrendObservation(13.8, null, T1));
    }

    @Test
    void nullOrderedAt_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new TrendObservation(13.8, "g/dL", null));
    }
}
