package com.bemo.shared.filter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bemo.shared.http.LoggingUtils;
import com.bemo.shared.http.RequestLogLine;
import com.bemo.shared.http.ResponseLogLine;
import com.bemo.shared.http.support.MultipleReadRequestWrapper;
import com.bemo.shared.logging.MdcDataProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Emits one structured access-log line per request ({@code type=request}) and one per response
 * ({@code type=response}). Bodies are only read for content types that are safe to replay
 * (JSON/XML/text) and are sanitised to a single line. Header values are masked.
 */
public class ServerLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ServerLoggingFilter.class);

    private static final Set<String> LOGGABLE_CONTENT_TYPES = Set.of(
            "application/json", "application/xml", "text/plain", "text/xml");

    private final boolean enabled;
    private final List<String> excludedHeaders;

    public ServerLoggingFilter(boolean enabled, List<String> excludedHeaders) {
        this.enabled = enabled;
        this.excludedHeaders = excludedHeaders == null ? List.of() : excludedHeaders;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled || shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean replayable = isReplayableContentType(request.getContentType());
        long start = System.nanoTime();
        if (replayable) {
            MultipleReadRequestWrapper wrapper = new MultipleReadRequestWrapper(request);
            logRequest(wrapper);
            filterChain.doFilter(wrapper, response);
        } else {
            logRequest(request);
            filterChain.doFilter(request, response);
        }
        long durationMs = (System.nanoTime() - start) / 1_000_000L;
        logResponse(response, durationMs);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        if (isAsyncDispatch(request)) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri == null || uri.startsWith("/actuator") || uri.equals("/favicon.ico");
    }

    private boolean isReplayableContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return true;
        }
        String base = contentType.toLowerCase();
        if (base.contains(";")) {
            base = base.substring(0, base.indexOf(';')).trim();
        }
        return LOGGABLE_CONTENT_TYPES.contains(base);
    }

    private void logRequest(HttpServletRequest request) {
        RequestLogLine line = new RequestLogLine(
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                MdcDataProvider.getRequestId(),
                MdcDataProvider.getTenantId(),
                MdcDataProvider.getUserId(),
                maskedHeaders(request),
                bodyOf(request));
        LOG.info(line.asJson());
    }

    private Map<String, String> maskedHeaders(HttpServletRequest request) {
        Map<String, String> headers = new java.util.HashMap<>();
        var names = request.getHeaderNames();
        if (names == null) {
            return headers;
        }
        Set<String> skip = new HashSet<>(excludedHeaders);
        names.asIterator().forEachRemaining(name -> {
            if (!skip.contains(name)) {
                headers.put(name, request.getHeader(name));
            }
        });
        return headers;
    }

    private String bodyOf(HttpServletRequest request) {
        if (request instanceof MultipleReadRequestWrapper wrapper) {
            return LoggingUtils.sanitizeBody(wrapper.getBody());
        }
        return null;
    }

    private void logResponse(HttpServletResponse response, long durationMs) {
        ResponseLogLine line = new ResponseLogLine(
                String.valueOf(response.getStatus()),
                response.getContentType(),
                durationMs,
                MdcDataProvider.getRequestId(),
                null);
        LOG.info(line.asJson());
    }
}
