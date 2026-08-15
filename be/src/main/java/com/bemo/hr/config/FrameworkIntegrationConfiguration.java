package com.bemo.hr.config;

import com.bemo.shared.annotation.EnablePerformanceMetricsLogging;
import com.bemo.shared.config.ConcurrencyConfiguration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Opt-in wiring for the shared framework's request-observability stack
 * (correlation-id tracing, MDC propagation, structured access logging, latency aspects).
 *
 * <p>Disabled by default so existing behaviour is unchanged. Enable with
 * {@code bemo.framework.enabled=true} in application.yml/properties.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "bemo.framework.enabled", havingValue = "true")
@EnablePerformanceMetricsLogging
@Import(ConcurrencyConfiguration.class)
public class FrameworkIntegrationConfiguration {
}
