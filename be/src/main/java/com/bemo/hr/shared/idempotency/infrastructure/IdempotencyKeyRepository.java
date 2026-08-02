package com.bemo.hr.shared.idempotency.infrastructure;

import com.bemo.hr.shared.idempotency.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
    Optional<IdempotencyKey> findByOperationTypeAndOperationId(String operationType, String operationId);
}
