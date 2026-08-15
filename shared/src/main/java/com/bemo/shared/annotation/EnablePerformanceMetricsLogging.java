package com.bemo.shared.annotation;

import com.bemo.shared.config.PerformanceMetricsLoggingConfiguration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Activates the performance-metrics stack: the method-latency and cache-latency aspects,
 * the tracing/MDC/request-logging filters and the shared logging configuration.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(PerformanceMetricsLoggingConfiguration.class)
public @interface EnablePerformanceMetricsLogging {
}
