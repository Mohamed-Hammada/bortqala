package com.bemo.shared.config;

import com.bemo.shared.aspect.CacheLatencyLoggingAspect;
import com.bemo.shared.aspect.MethodPerformanceLoggingAspect;
import com.bemo.shared.aspect.ScheduleLatencyLoggingAspect;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the latency-logging aspects. All three aspects honour the shared
 * {@code shared.performance-logging.default.*} settings and can be switched off together.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PerformanceLoggingProperties.class)
public class LatencyLoggingConfiguration {

    @Bean
    @ConditionalOnProperty(name = "shared.performance-logging.default.enabled", havingValue = "true", matchIfMissing = true)
    public MethodPerformanceLoggingAspect methodPerformanceLoggingAspect(PerformanceLoggingProperties properties) {
        return new MethodPerformanceLoggingAspect(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "shared.performance-logging.default.enabled", havingValue = "true", matchIfMissing = true)
    public CacheLatencyLoggingAspect cacheLatencyLoggingAspect(PerformanceLoggingProperties properties) {
        return new CacheLatencyLoggingAspect(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "shared.performance-logging.default.enabled", havingValue = "true", matchIfMissing = true)
    public ScheduleLatencyLoggingAspect scheduleLatencyLoggingAspect(PerformanceLoggingProperties properties) {
        return new ScheduleLatencyLoggingAspect(properties);
    }
}
