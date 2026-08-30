package com.bemo.hr.platform.infrastructure;

import com.bemo.hr.platform.application.ApiKeyService;
import com.bemo.hr.platform.domain.ApiKey;
import com.bemo.hr.platform.domain.ApiKeyRepository;
import com.bemo.hr.shared.security.ApiKeyAuthentication;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

/**
 * Authenticates machine-to-machine calls carrying the {@code X-Api-Key} header.
 * <p>
 * The full key embeds its app id: {@code bk_<appId>_<randomHex>}. The filter splits it, binds the tenant,
 * resolves the stored SHA-256 hash, enforces the key's rate limit and scope-to-path matching, then binds an
 * {@link ApiKeyAuthentication}. Requests without the header (or with an already-authenticated principal,
 * e.g. a bearer token) pass straight through.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Api-Key";
    private static final String API_KEY_PREFIX = "bk_";

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyRateLimiter rateLimiter;

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository, ApiKeyRateLimiter rateLimiter) {
        this.apiKeyRepository = apiKeyRepository;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String raw = request.getHeader(API_KEY_HEADER);
        if (raw == null || raw.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated() && !(existing instanceof ApiKeyAuthentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        String appId = parseAppId(raw);
        if (appId == null) {
            deny(response, HttpStatus.UNAUTHORIZED, "APIKEY_INVALID", "Malformed API key.");
            return;
        }

        TenantContext.set(appId);
        try {
            ApiKey apiKey = apiKeyRepository.findByAppIdAndKeyHash(appId, ApiKeyService.sha256(raw)).orElse(null);
            if (apiKey == null) {
                deny(response, HttpStatus.UNAUTHORIZED, "APIKEY_INVALID", "Unknown API key.");
                return;
            }
            if (!apiKey.isActive()) {
                deny(response, HttpStatus.UNAUTHORIZED, "APIKEY_INACTIVE", "API key is revoked or disabled.");
                return;
            }

            String requiredScope = requiredScope(request.getRequestURI());
            if (!hasScope(apiKey, requiredScope)) {
                deny(response, HttpStatus.FORBIDDEN, "APIKEY_SCOPE_DENIED",
                        "API key lacks the scope required for this resource.");
                return;
            }

            if (!rateLimiter.tryAcquire(appId, apiKey.getId(), apiKey.getRateLimitPerMin(), Instant.now())) {
                response.setHeader("Retry-After", "60");
                deny(response, HttpStatus.TOO_MANY_REQUESTS, "APIKEY_RATE_LIMITED",
                        "API key rate limit exceeded.");
                return;
            }

            apiKey.setLastUsedAt(Instant.now());
            apiKeyRepository.save(apiKey);

            SecurityContextHolder.getContext().setAuthentication(new ApiKeyAuthentication(apiKey));
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }
    }

    /** Splits {@code bk_<appId>_<randomHex>} on the last underscore so app ids may contain underscores. */
    public static String parseAppId(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(API_KEY_PREFIX)) {
            return null;
        }
        String rest = rawKey.substring(API_KEY_PREFIX.length());
        int separator = rest.lastIndexOf('_');
        if (separator < 1) {
            return null;
        }
        String appId = rest.substring(0, separator);
        String secret = rest.substring(separator + 1);
        if (appId.isBlank() || secret.length() < 16) {
            return null;
        }
        return appId;
    }

    /** Derives the required scope from the first URI segment under {@code /api/v1/} (e.g. {@code invoices}). */
    static String requiredScope(String uri) {
        if (uri == null) {
            return null;
        }
        String path = uri;
        if (path.startsWith("/api/v1/")) {
            path = path.substring("/api/v1/".length());
        }
        int slash = path.indexOf('/');
        String segment = slash < 0 ? path : path.substring(0, slash);
        return segment.isBlank() ? null : segment;
    }

    private static boolean hasScope(ApiKey apiKey, String requiredScope) {
        if (requiredScope == null) {
            return true;
        }
        Set<String> scopes = apiKey.scopeSet();
        if (scopes.contains("*")) {
            return true;
        }
        return scopes.stream().anyMatch(scope -> scope.equals(requiredScope) || scope.startsWith(requiredScope + ":"));
    }

    private static void deny(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\""
                + message.replace("\"", "'") + "\"}");
    }
}