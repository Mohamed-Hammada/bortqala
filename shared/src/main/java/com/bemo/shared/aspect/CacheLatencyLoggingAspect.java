package com.bemo.shared.aspect;

import com.bemo.shared.config.PerformanceLoggingProperties;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Times every Spring cache operation ({@code @Cacheable}, {@code @CachePut}, {@code @CacheEvict})
 * and reports hit/miss plus duration. Useful to spot cache misses against Redis-backed caches.
 */
@Aspect
public class CacheLatencyLoggingAspect {

    private static final Logger LOG = LoggerFactory.getLogger(CacheLatencyLoggingAspect.class);

    private final PerformanceLoggingProperties properties;

    public CacheLatencyLoggingAspect(PerformanceLoggingProperties properties) {
        this.properties = properties;
    }

    @Around("@annotation(org.springframework.cache.annotation.Cacheable) "
            + "|| @annotation(org.springframework.cache.annotation.CachePut) "
            + "|| @annotation(org.springframework.cache.annotation.CacheEvict)")
    public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        Object result = joinPoint.proceed();
        long durationMs = (System.nanoTime() - start) / 1_000_000L;
        if (LOG.isDebugEnabled() || durationMs >= properties.minDurationInMillis()) {
            LOG.warn("Cache operation [{}] took {} ms", signature(joinPoint), durationMs);
        }
        return result;
    }

    private String signature(ProceedingJoinPoint joinPoint) {
        if (joinPoint.getSignature() instanceof MethodSignature methodSignature) {
            return methodSignature.getDeclaringType().getSimpleName() + '.' + methodSignature.getName();
        }
        return joinPoint.getSignature().toShortString();
    }
}
