package com.bemo.shared.aspect;

import com.bemo.shared.annotation.MeasureMethodLatency;
import com.bemo.shared.config.PerformanceLoggingProperties;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Around advice that times methods annotated with {@link MeasureMethodLatency} and logs
 * any execution slower than the configured threshold. The slow-call message is logged at
 * WARN, everything else at DEBUG so production logs stay quiet by default.
 */
@Aspect
public class MethodPerformanceLoggingAspect {

    private static final Logger LOG = LoggerFactory.getLogger(MethodPerformanceLoggingAspect.class);

    private final PerformanceLoggingProperties properties;

    public MethodPerformanceLoggingAspect(PerformanceLoggingProperties properties) {
        this.properties = properties;
    }

    @Around("@annotation(com.bemo.shared.annotation.MeasureMethodLatency)")
    public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000L;
            if (durationMs >= properties.minDurationInMillis()) {
                LOG.warn("Method [{}] took {} ms (threshold {} ms)",
                        signature(joinPoint), durationMs, properties.minDurationInMillis());
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Method [{}] took {} ms",
                        signature(joinPoint), durationMs);
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
