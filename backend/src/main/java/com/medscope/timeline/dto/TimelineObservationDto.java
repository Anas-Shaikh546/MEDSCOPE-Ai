package com.medscope.timeline.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One historical data point as seen by the client. Carries both the
 * resolved ordering date and a flag telling the UI whether that date
 * is a confirmed lab date (testDate) or merely the upload date
 * (createdAt fallback) - the UI must distinguish these clearly rather
 * than implying a confirmed test date exists when it doesn't (6.2).
 *
 * Source traceability (6.16): reportId and reportResultId let the UI
 * link "where did this number come from?" back to the original PDF.
 */
public record TimelineObservationDto(
        LocalDate date,
        boolean dateIsConfirmed,
        Long reportId,
        Long reportResultId,
        double value,
        String unit,
        Double referenceLow,
        Double referenceHigh,
        String status
) {
}