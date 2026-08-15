package com.bemo.shared.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public method for latency measurement. When performance logging is enabled,
 * {@code MethodPerformanceLoggingAspect} records the execution time and logs any call
 * slower than the configured threshold.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface MeasureMethodLatency {
}
