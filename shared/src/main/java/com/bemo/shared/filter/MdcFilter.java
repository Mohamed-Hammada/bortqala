package com.bemo.shared.filter;

import java.io.IOException;
import java.util.Map;

import com.bemo.shared.http.CorrelationHeaders;
import com.bemo.shared.logging.MdcDataProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Propagates inbound correlation headers into the SLF4J MDC so every log line emitted during
 * the request carries tenant/user/branch context automatically. Entries are cleared in
 * {@code finally} so pooled threads never leak context between requests.
 */
public class MdcFilter extends OncePerRequestFilter {

    private static final Map<String, String> HEADER_TO_MDC = Map.of(
            CorrelationHeaders.TRANSACTION_ID, MdcDataProvider.TRANSACTION_ID,
            CorrelationHeaders.USER_ID, MdcDataProvider.USER_ID,
            CorrelationHeaders.TENANT_ID, MdcDataProvider.TENANT_ID,
            CorrelationHeaders.BRANCH_ID, MdcDataProvider.BRANCH_ID,
            CorrelationHeaders.CUSTOMER_ID, MdcDataProvider.CUSTOMER_ID,
            CorrelationHeaders.EXTERNAL_ID, MdcDataProvider.EXTERNAL_ID);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            HEADER_TO_MDC.forEach((header, mdcKey) -> {
                String value = request.getHeader(header);
                if (value != null && !value.isBlank()) {
                    MDC.put(mdcKey, value);
                }
            });
            filterChain.doFilter(request, response);
        } finally {
            HEADER_TO_MDC.values().forEach(MDC::remove);
        }
    }
}
