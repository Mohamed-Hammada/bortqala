package com.bemo.shared.aspect;

import com.bemo.shared.config.PerformanceLoggingProperties;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Times every scheduled job and logs executions slower than the configured threshold.
 */
@Aspect
public class ScheduleLatencyLoggingAspect {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduleLatencyLoggingAspect.class);

    private final PerformanceLoggingProperties properties;

    public ScheduleLatencyLoggingAspect(PerformanceLoggingProperties properties) {
        this.properties = properties;
    }

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000L;
            if (durationMs >= properties.minDurationInMillis()) {
                LOG.warn("Scheduled job [{}] took {} ms (threshold {} ms)",
                        signature(joinPoint), durationMs, properties.minDurationInMillis());
            }
        }
    }

    private String signature(ProceedingJoinPoint joinPoint) {
        if (joinPoint.getSignature() instanceof MethodSignature methodSignature) {
            return methodSignature.getDeclaringType().getSimpleName() + '.' + methodSignature.getName();
        }
        return joinPoint.getSignature().toShortString();
    }
}
