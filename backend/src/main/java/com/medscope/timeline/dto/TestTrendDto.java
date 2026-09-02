package com.medscope.timeline.dto;

import com.medscope.timeline.trend.TrendDirection;

import java.util.List;

/**
 * The trend for one canonical test across all of the authenticated
 * user's processed reports.
 */
public record TestTrendDto(
        String canonicalName,
        String displayName,
        String category,
        String defaultUnit,
        TrendDirection trend,
        List<TimelineObservationDto> observations
) {
}