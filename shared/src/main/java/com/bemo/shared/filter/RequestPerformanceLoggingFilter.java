package com.bemo.shared.filter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.bemo.shared.http.MetricTypes;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Times every HTTP request. When a {@link MeterRegistry} is available the duration is recorded
 * as a {@code http.server.requests} timer; the slowest requests are additionally logged.
 */
public class RequestPerformanceLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestPerformanceLoggingFilter.class);

    private final MeterRegistry meterRegistry;
    private final long slowThresholdMillis;

    public RequestPerformanceLoggingFilter(MeterRegistry meterRegistry, long slowThresholdMillis) {
        this.meterRegistry = meterRegistry;
        this.slowThresholdMillis = slowThresholdMillis;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000L;
            record(request, response, durationMs);
        }
    }

    private void record(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        if (meterRegistry != null) {
            Timer.builder("http.server.requests")
                    .description("Duration of HTTP requests")
                    .tags("uri", safeUri(request.getRequestURI()),
                            "status", String.valueOf(response.getStatus()))
                    .register(meterRegistry)
                    .record(durationMs, TimeUnit.MILLISECONDS);
            meterRegistry.counter(MetricTypes.API_REQUEST_COUNTER, "uri", safeUri(request.getRequestURI()))
                    .increment();
        }
        if (durationMs >= slowThresholdMillis) {
            LOG.warn("Slow HTTP request [{} {}] took {} ms",
                    request.getMethod(), request.getRequestURI(), durationMs);
        }
    }

    private String safeUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "/unknown";
        }
        return uri.length() > 100 ? uri.substring(0, 100) : uri;
    }
}
