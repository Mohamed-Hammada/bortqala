package com.bemo.hr.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.bemo.hr.shared.security.TenantContext;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class RequestAuditFilter extends OncePerRequestFilter {
    public static final String CLIENT_CORRELATION_HEADER = "X-Correlation-Id";
    public static final String SERVER_CORRELATION_HEADER = "X-Server-Correlation-Id";
    public static final String DEVICE_HEADER = "X-Device-Id";
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9._-]{1,100}");
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestAuditFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientCorrelationId = safeOrGenerated(request.getHeader(CLIENT_CORRELATION_HEADER));
        String serverCorrelationId = UUID.randomUUID().toString();
        String deviceId = safeOrUnknown(request.getHeader(DEVICE_HEADER));
        long started = System.nanoTime();
        response.setHeader(CLIENT_CORRELATION_HEADER, clientCorrelationId);
        response.setHeader(SERVER_CORRELATION_HEADER, serverCorrelationId);
        MDC.put("clientCorrelationId", clientCorrelationId);
        MDC.put("serverCorrelationId", serverCorrelationId);
        MDC.put("deviceId", deviceId);
        Authentication requestAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (requestAuthentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            String appId = jwtAuthenticationToken.getToken().getClaimAsString("appId");
            if (appId != null) TenantContext.set(appId);
            MDC.put("appId", appId == null ? "unknown" : appId);
            MDC.put("appCode", valueOrUnknown(jwtAuthenticationToken.getToken().getClaimAsString("appCode")));
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean authenticated = authentication != null && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken);
            String username = authenticated ? authentication.getName() : "anonymous";
            String userId = authentication instanceof JwtAuthenticationToken jwt
                    ? jwt.getToken().getClaimAsString("userId") : "unknown";
            String roles = authentication == null ? "" : authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .sorted().collect(java.util.stream.Collectors.joining(","));
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            MDC.put("userId", userId == null ? "unknown" : userId);
            MDC.put("username", username);
            MDC.put("clientIp", request.getRemoteAddr());
            LOGGER.atInfo()
                    .addKeyValue("event", "http_request")
                    .addKeyValue("httpMethod", request.getMethod())
                    .addKeyValue("path", request.getRequestURI())
                    .addKeyValue("status", response.getStatus())
                    .addKeyValue("durationMs", durationMs)
                    .addKeyValue("roles", roles)
                    .addKeyValue("userAgent", truncate(request.getHeader("User-Agent"), 200))
                    .log("HTTP request completed");
            TenantContext.clear();
            MDC.clear();
        }
    }

    private String safeOrGenerated(String value) {
        return value != null && SAFE_IDENTIFIER.matcher(value).matches() ? value : UUID.randomUUID().toString();
    }

    private String safeOrUnknown(String value) {
        return value != null && SAFE_IDENTIFIER.matcher(value).matches() ? value : "unknown";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "unknown";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String valueOrUnknown(String value) { return value == null ? "unknown" : value; }
}
