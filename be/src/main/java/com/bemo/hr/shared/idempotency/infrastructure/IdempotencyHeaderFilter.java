package com.bemo.hr.shared.idempotency.infrastructure;

import com.bemo.hr.shared.idempotency.application.IdempotencyService;
import com.bemo.hr.shared.idempotency.domain.IdempotencyKey;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class IdempotencyHeaderFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    public static final String ALT_IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    public static final String REPLAYED_HEADER = "X-Idempotency-Replayed";
    private static final Duration DEFAULT_LEASE = Duration.ofSeconds(60);

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public IdempotencyHeaderFilter(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String rawKey = request.getHeader(IDEMPOTENCY_HEADER);
        if (rawKey == null || rawKey.isBlank()) {
            rawKey = request.getHeader(ALT_IDEMPOTENCY_HEADER);
        }

        if (rawKey == null || rawKey.isBlank() || isReadOnlyMethod(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = rawKey.trim();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 1024 * 1024);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        // Pre-read request body if cached
        byte[] bodyBytes = wrappedRequest.getInputStream().readAllBytes();
        String requestBody = new String(bodyBytes, StandardCharsets.UTF_8);

        String appId = TenantContext.currentOrSystem();
        String operationType = "HTTP:" + request.getMethod() + ":" + request.getRequestURI();
        String requestHash = IdempotencyService.hash(appId + "|" + request.getMethod() + "|" + request.getRequestURI() + "|" + requestBody);

        Instant now = Instant.now();
        String ownerToken = UUID.randomUUID().toString();

        Optional<IdempotencyKey> existingOpt = idempotencyKeyRepository.findByOperationTypeAndOperationId(operationType, idempotencyKey);
        if (existingOpt.isPresent()) {
            IdempotencyKey existing = existingOpt.get();
            if (IdempotencyKey.STATUS_COMPLETED.equals(existing.getStatus())) {
                if (!existing.getRequestHash().equals(requestHash)) {
                    sendConflict(response, "IDEMPOTENCY_HASH_MISMATCH", "The same idempotency key was already used with a different request payload.");
                    return;
                }
                replayStoredResponse(response, existing.getResponseReferenceOrBody());
                return;
            }

            if (!existing.getRequestHash().equals(requestHash)) {
                sendConflict(response, "IDEMPOTENCY_HASH_MISMATCH", "The same idempotency key was already used with a different request payload.");
                return;
            }

            boolean retryAvailable = IdempotencyKey.STATUS_FAILED.equals(existing.getStatus())
                    || (IdempotencyKey.STATUS_IN_PROGRESS.equals(existing.getStatus())
                    && existing.getLeaseExpiresAt() != null && existing.getLeaseExpiresAt().isBefore(now));

            if (!retryAvailable || idempotencyKeyRepository.steal(appId, operationType, idempotencyKey, requestHash, now.plus(DEFAULT_LEASE), now, ownerToken) != 1) {
                sendConflict(response, "IDEMPOTENCY_IN_PROGRESS", "The operation is currently being processed.");
                return;
            }
        } else {
            int reserved = idempotencyKeyRepository.reserve(UUID.randomUUID().toString(), appId, operationType, idempotencyKey, requestHash, now.plus(DEFAULT_LEASE), ownerToken);
            if (reserved != 1) {
                sendConflict(response, "IDEMPOTENCY_IN_PROGRESS", "The operation is currently being processed.");
                return;
            }
        }

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            int statusCode = wrappedResponse.getStatus();
            byte[] responseBytes = wrappedResponse.getContentAsByteArray();
            String responseBody = new String(responseBytes, StandardCharsets.UTF_8);

            if (statusCode >= 200 && statusCode < 400) {
                String serialized = statusCode + "||" + responseBody;
                idempotencyKeyRepository.complete(appId, operationType, idempotencyKey, ownerToken, serialized);
            } else {
                idempotencyKeyRepository.fail(appId, operationType, idempotencyKey, ownerToken);
            }

            wrappedResponse.copyBodyToResponse();
        } catch (Exception ex) {
            idempotencyKeyRepository.fail(appId, operationType, idempotencyKey, ownerToken);
            throw ex;
        }
    }

    private boolean isReadOnlyMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method);
    }

    private void replayStoredResponse(HttpServletResponse response, String stored) throws IOException {
        int status = HttpServletResponse.SC_OK;
        String body = stored;
        if (stored != null && stored.contains("||")) {
            int idx = stored.indexOf("||");
            try {
                status = Integer.parseInt(stored.substring(0, idx));
                body = stored.substring(idx + 2);
            } catch (NumberFormatException ignored) {
            }
        }
        response.setStatus(status);
        response.setHeader(REPLAYED_HEADER, "true");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (body != null) {
            response.getWriter().write(body);
            response.getWriter().flush();
        }
    }

    private void sendConflict(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpStatus.CONFLICT.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String payload = String.format("{\"errorCode\":\"%s\",\"message\":\"%s\"}", code, message);
        response.getWriter().write(payload);
        response.getWriter().flush();
    }
}
