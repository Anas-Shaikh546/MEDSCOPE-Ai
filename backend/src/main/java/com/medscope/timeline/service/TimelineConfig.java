package com.medscope.timeline.service;

import com.medscope.timeline.trend.TrendCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TrendCalculator is a pure-logic class with no Spring annotations
 * (intentional: it must be testable without a Spring context). This
 * config makes it injectable as a normal Spring bean.
 */
@Configuration
public class TimelineConfig {

    @Bean
    public TrendCalculator trendCalculator() {
        return new TrendCalculator();
    }
}