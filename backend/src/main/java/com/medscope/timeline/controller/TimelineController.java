package com.medscope.timeline.controller;

import com.medscope.common.exception.ResourceNotFoundException;
import com.medscope.security.CurrentUser;
import com.medscope.timeline.dto.TestTrendDto;
import com.medscope.timeline.dto.TrendsResponse;
import com.medscope.timeline.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * All endpoints derive the user from the JWT via @CurrentUser - never
 * from a request parameter (6.17).
 */
@RestController
@RequestMapping("/results/trends")
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;

    /**
     * All trends for the authenticated user - one entry per canonical
     * test name for which they have at least one processed numeric result.
     * Empty list (not 404) when the user has no results yet.
     */
    @GetMapping
    public TrendsResponse getAllTrends(@CurrentUser Long authenticatedUserId) {
        return timelineService.getAllTrends(authenticatedUserId);
    }

    /**
     * Trend for one specific test by canonical name (e.g. "hemoglobin",
     * "wbc", "total_cholesterol"). 404 when this user has no results for
     * the requested test - same "not found vs not yours" merge as every
     * other per-user resource endpoint (no enumeration signal).
     */
    @GetMapping("/{canonicalName}")
    public TestTrendDto getTrendForTest(
            @CurrentUser Long authenticatedUserId,
            @PathVariable String canonicalName
    ) {
        return timelineService.getTrendForTest(authenticatedUserId, canonicalName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No results found for test: " + canonicalName));
    }
}