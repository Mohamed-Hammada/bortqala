package com.bemo.shared.filter;

import java.io.IOException;
import java.util.UUID;

import com.bemo.shared.logging.MdcDataProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a correlation (request) id to every request and echoes it back in the response
 * header so clients can trace end-to-end. Inbound correlation ids are honoured; missing ones
 * are generated. The id lives in MDC for the full request span.
 */
public class TracingFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final String correlationIdHeader;

    public TracingFilter(boolean enabled, String correlationIdHeader) {
        this.enabled = enabled;
        this.correlationIdHeader = correlationIdHeader == null || correlationIdHeader.isBlank()
                ? "web-correlation-id"
                : correlationIdHeader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }
        String inbound = request.getHeader(correlationIdHeader);
        String correlationId = (inbound != null && !inbound.isBlank())
                ? inbound
                : UUID.randomUUID().toString();
        response.setHeader(correlationIdHeader, correlationId);
        MDC.put(MdcDataProvider.REQUEST_ID, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MdcDataProvider.REQUEST_ID);
        }
    }
}
