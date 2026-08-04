package com.bemo.hr.shared.idempotency.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.idempotency.domain.IdempotencyKey;
import com.bemo.hr.shared.idempotency.infrastructure.IdempotencyKeyRepository;
import com.bemo.hr.shared.security.TenantContext;
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

@Service
public class IdempotencyService {

    static final Duration IN_PROGRESS_LEASE = Duration.ofSeconds(60);

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    /**
     * Reservation-based idempotency. The key is claimed with an atomic
     * {@code INSERT ... ON CONFLICT DO NOTHING} so concurrent duplicates never
     * depend on a post-failure re-read. A reservation that was never completed
     * (for example after a process crash) expires after {@value #IN_PROGRESS_LEASE}
     * and can be claimed by the next retry.
     */
    public <T> T execute(String operationType, String operationId, String requestHash,
                         Supplier<T> operation, Function<T, String> referenceWriter, Function<String, T> replayMapper) {
        Instant now = Instant.now();
        if (reserve(operationType, operationId, requestHash, now.plus(IN_PROGRESS_LEASE))) {
            return runAndRecord(operationType, operationId, requestHash, operation, referenceWriter);
        }
        IdempotencyKey existing = idempotencyKeyRepository
                .findByOperationTypeAndOperationId(operationType, operationId)
                .orElseThrow(() -> inProgress());
        if (IdempotencyKey.STATUS_COMPLETED.equals(existing.getStatus())) {
            return replay(existing, requestHash, replayMapper);
        }
        if (isAvailableForRetry(existing, now)
                && steal(operationType, operationId, requestHash, now.plus(IN_PROGRESS_LEASE), now)) {
            return runAndRecord(operationType, operationId, requestHash, operation, referenceWriter);
        }
        throw inProgress();
    }

    private <T> T runAndRecord(String operationType, String operationId, String requestHash,
                               Supplier<T> operation, Function<T, String> referenceWriter) {
        try {
            T result = operation.get();
            idempotencyKeyRepository.complete(TenantContext.currentOrSystem(), operationType, operationId,
                    referenceWriter.apply(result));
            return result;
        } catch (RuntimeException exception) {
            idempotencyKeyRepository.fail(TenantContext.currentOrSystem(), operationType, operationId);
            throw exception;
        }
    }

    private boolean reserve(String operationType, String operationId, String requestHash, Instant leaseExpiresAt) {
        return idempotencyKeyRepository.reserve(UUID.randomUUID().toString(), TenantContext.currentOrSystem(),
                operationType, operationId, requestHash, leaseExpiresAt) == 1;
    }

    private boolean steal(String operationType, String operationId, String requestHash,
                          Instant leaseExpiresAt, Instant now) {
        return idempotencyKeyRepository.steal(TenantContext.currentOrSystem(), operationType, operationId,
                requestHash, leaseExpiresAt, now) == 1;
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
            throw new BusinessRuleException("The same operation key was already used with a different request.",
                    "IDEMPOTENCY_HASH_MISMATCH", HttpStatus.CONFLICT);
        }
        return replayMapper.apply(key.getResponseReferenceOrBody());
    }

    private BusinessRuleException inProgress() {
        return new BusinessRuleException("The operation is already being processed.",
                "IDEMPOTENCY_IN_PROGRESS", HttpStatus.CONFLICT);
    }

    public static String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
