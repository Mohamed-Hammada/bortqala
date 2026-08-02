package com.bemo.hr.shared.idempotency.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.idempotency.domain.IdempotencyKey;
import com.bemo.hr.shared.idempotency.infrastructure.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    public <T> T execute(String operationType, String operationId, String requestHash,
                         Supplier<T> operation, Function<T, String> referenceWriter, Function<String, T> replayMapper) {
        Optional<IdempotencyKey> existing = idempotencyKeyRepository
                .findByOperationTypeAndOperationId(operationType, operationId);
        if (existing.isPresent()) {
            IdempotencyKey key = existing.get();
            if (IdempotencyKey.STATUS_COMPLETED.equals(key.getStatus())) {
                if (!key.getRequestHash().equals(requestHash)) {
                    throw new BusinessRuleException("The same operation key was already used with a different request.");
                }
                return replayMapper.apply(key.getResponseReferenceOrBody());
            }
            throw new BusinessRuleException("The operation is already being processed.");
        }
        IdempotencyKey key = new IdempotencyKey(operationType, operationId, requestHash);
        idempotencyKeyRepository.save(key);
        try {
            T result = operation.get();
            key.complete(referenceWriter.apply(result));
            idempotencyKeyRepository.save(key);
            return result;
        } catch (RuntimeException exception) {
            key.fail();
            idempotencyKeyRepository.save(key);
            throw exception;
        }
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
