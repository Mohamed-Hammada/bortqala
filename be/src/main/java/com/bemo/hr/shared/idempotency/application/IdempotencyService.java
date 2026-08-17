package com.bemo.hr.shared.idempotency.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.idempotency.domain.IdempotencyKey;
import com.bemo.hr.shared.idempotency.infrastructure.IdempotencyKeyRepository;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Service
public class IdempotencyService {

    static final Duration IN_PROGRESS_LEASE = Duration.ofSeconds(60);

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    public static String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    /**
     * Reservation-based idempotency. The key is claimed with an atomic
     * {@code INSERT ... ON CONFLICT DO NOTHING} so concurrent duplicates never
     * depend on a post-failure re-read. The winning caller keeps an opaque owner
     * token; only that owner can complete or fail the reservation, and a
     * long-running owner can renew its lease with {@link #renewLease}. A
     * reservation that was never completed (for example after a process crash)
     * expires after {@value #IN_PROGRESS_LEASE} and can be claimed by the next
     * retry, but only when the retry carries the exact same request hash.
     */
    public <T> T execute(String operationType, String operationId, String requestHash,
                         Supplier<T> operation, Function<T, String> referenceWriter, Function<String, T> replayMapper) {
        log.debug("execute called with operationType={}, operationId={}", operationType, operationId);
        Instant now = Instant.now();
        String ownerToken = UUID.randomUUID().toString();
        if (reserve(operationType, operationId, requestHash, ownerToken, now.plus(IN_PROGRESS_LEASE))) {
            return runAndRecord(operationType, operationId, requestHash, ownerToken, operation, referenceWriter);
        }
        IdempotencyKey existing = idempotencyKeyRepository
                .findByOperationTypeAndOperationId(operationType, operationId)
                .orElseThrow(() -> inProgress());
        if (IdempotencyKey.STATUS_COMPLETED.equals(existing.getStatus())) {
            return replay(existing, requestHash, replayMapper);
        }
        if (!existing.getRequestHash().equals(requestHash)) {
            throw hashMismatch();
        }
        if (isAvailableForRetry(existing, now)
                && steal(operationType, operationId, requestHash, ownerToken, now.plus(IN_PROGRESS_LEASE), now)) {
            return runAndRecord(operationType, operationId, requestHash, ownerToken, operation, referenceWriter);
        }
        throw inProgress();
    }

    /**
     * Lets the owning attempt extend its IN_PROGRESS lease while the operation is
     * still running, so a slow batch is not stolen mid-flight.
     */
    public void renewLease(String operationType, String operationId, String ownerToken) {
        log.debug("renewLease called with operationType={}, operationId={}", operationType, operationId);
        if (ownerToken == null) {
            return;
        }
        idempotencyKeyRepository.renewLease(TenantContext.currentOrSystem(), operationType, operationId,
                ownerToken, Instant.now().plus(IN_PROGRESS_LEASE));
    }

    private <T> T runAndRecord(String operationType, String operationId, String requestHash,
                               String ownerToken, Supplier<T> operation, Function<T, String> referenceWriter) {
        try {
            T result = operation.get();
            idempotencyKeyRepository.complete(TenantContext.currentOrSystem(), operationType, operationId,
                    ownerToken, referenceWriter.apply(result));
            return result;
        } catch (RuntimeException exception) {
            idempotencyKeyRepository.fail(TenantContext.currentOrSystem(), operationType, operationId, ownerToken);
            throw exception;
        }
    }

    private boolean reserve(String operationType, String operationId, String requestHash,
                            String ownerToken, Instant leaseExpiresAt) {
        return idempotencyKeyRepository.reserve(UUID.randomUUID().toString(), TenantContext.currentOrSystem(),
                operationType, operationId, requestHash, leaseExpiresAt, ownerToken) == 1;
    }

    private boolean steal(String operationType, String operationId, String requestHash,
                          String ownerToken, Instant leaseExpiresAt, Instant now) {
        return idempotencyKeyRepository.steal(TenantContext.currentOrSystem(), operationType, operationId,
                requestHash, leaseExpiresAt, now, ownerToken) == 1;
    }

    private boolean isAvailableForRetry(IdempotencyKey key, Instant now) {
        if (IdempotencyKey.STATUS_FAILED.equals(key.getStatus())) {
            return true;
        }
        return IdempotencyKey.STATUS_IN_PROGRESS.equals(key.getStatus())
                && (key.getLeaseExpiresAt() == null || key.getLeaseExpiresAt().isBefore(now));
    }

    private <T> T replay(IdempotencyKey key, String requestHash, Function<String, T> replayMapper) {
        if (!key.getRequestHash().equals(requestHash)) {
            throw hashMismatch();
        }
        return replayMapper.apply(key.getResponseReferenceOrBody());
    }

    private BusinessRuleException hashMismatch() {
        return new BusinessRuleException("The same operation key was already used with a different request.",
                "IDEMPOTENCY_HASH_MISMATCH", HttpStatus.CONFLICT);
    }

    private BusinessRuleException inProgress() {
        return new BusinessRuleException("The operation is already being processed.",
                "IDEMPOTENCY_IN_PROGRESS", HttpStatus.CONFLICT);
    }
}
