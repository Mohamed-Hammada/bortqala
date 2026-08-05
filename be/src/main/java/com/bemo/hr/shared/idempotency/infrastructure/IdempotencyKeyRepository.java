package com.bemo.hr.shared.idempotency.infrastructure;

import com.bemo.hr.shared.idempotency.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
    Optional<IdempotencyKey> findByOperationTypeAndOperationId(String operationType, String operationId);

    /**
     * Atomically reserves an idempotency key with {@code ON CONFLICT DO NOTHING}.
     * The winning caller records an opaque owner token and attempt 1. Returns
     * {@code 1} when this caller owns the reservation and {@code 0} when the key
     * already exists. Runs in its own committed transaction so concurrent callers
     * never observe a half-visible reservation and a crashed caller leaves a stale
     * IN_PROGRESS row that can expire.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query(value = """
            INSERT INTO idempotency_keys (
                id, app_id, operation_type, operation_id, request_hash, status,
                lease_expires_at, owner_token, attempt_number
            ) VALUES (
                :id, :appId, :operationType, :operationId, :requestHash, 'IN_PROGRESS',
                :leaseExpiresAt, :ownerToken, 1
            )
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int reserve(@Param("id") String id, @Param("appId") String appId,
                @Param("operationType") String operationType, @Param("operationId") String operationId,
                @Param("requestHash") String requestHash, @Param("leaseExpiresAt") Instant leaseExpiresAt,
                @Param("ownerToken") String ownerToken);

    /**
     * Re-reserves a key whose reservation expired (or a FAILED attempt) only when
     * the retry carries the same request hash, so a stale owner is never handed a
     * different payload. Only one concurrent stealer can match the predicate
     * because the winning UPDATE extends the lease in the same committed statement.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query(value = """
            UPDATE idempotency_keys
            SET lease_expires_at = :leaseExpiresAt,
                owner_token = :ownerToken,
                attempt_number = attempt_number + 1,
                status = 'IN_PROGRESS',
                completed_at = NULL
            WHERE app_id = :appId
              AND operation_type = :operationType
              AND operation_id = :operationId
              AND request_hash = :requestHash
              AND (status = 'FAILED' OR (status = 'IN_PROGRESS' AND lease_expires_at < :now))
            """, nativeQuery = true)
    int steal(@Param("appId") String appId, @Param("operationType") String operationType,
              @Param("operationId") String operationId, @Param("requestHash") String requestHash,
              @Param("leaseExpiresAt") Instant leaseExpiresAt, @Param("now") Instant now,
              @Param("ownerToken") String ownerToken);

    /**
     * Heartbeat for a long-running owner: pushes the lease out so the reservation
     * is not stolen while the owning attempt is still making progress.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query(value = """
            UPDATE idempotency_keys
            SET lease_expires_at = :leaseExpiresAt
            WHERE app_id = :appId
              AND operation_type = :operationType
              AND operation_id = :operationId
              AND owner_token = :ownerToken
              AND status = 'IN_PROGRESS'
            """, nativeQuery = true)
    int renewLease(@Param("appId") String appId, @Param("operationType") String operationType,
                   @Param("operationId") String operationId, @Param("ownerToken") String ownerToken,
                   @Param("leaseExpiresAt") Instant leaseExpiresAt);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query(value = """
            UPDATE idempotency_keys
            SET status = 'COMPLETED',
                response_reference_or_body = :reference,
                completed_at = CURRENT_TIMESTAMP
            WHERE app_id = :appId
              AND operation_type = :operationType
              AND operation_id = :operationId
              AND owner_token = :ownerToken
            """, nativeQuery = true)
    int complete(@Param("appId") String appId, @Param("operationType") String operationType,
                 @Param("operationId") String operationId, @Param("ownerToken") String ownerToken,
                 @Param("reference") String reference);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query(value = """
            UPDATE idempotency_keys
            SET status = 'FAILED',
                completed_at = CURRENT_TIMESTAMP
            WHERE app_id = :appId
              AND operation_type = :operationType
              AND operation_id = :operationId
              AND owner_token = :ownerToken
            """, nativeQuery = true)
    int fail(@Param("appId") String appId, @Param("operationType") String operationType,
             @Param("operationId") String operationId, @Param("ownerToken") String ownerToken);
}
