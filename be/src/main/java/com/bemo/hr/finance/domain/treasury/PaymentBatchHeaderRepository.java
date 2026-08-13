package com.bemo.hr.finance.domain.treasury;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentBatchHeaderRepository extends JpaRepository<PaymentBatchHeader, String> {
    Optional<PaymentBatchHeader> findByBatchNumber(String batchNumber);
    List<PaymentBatchHeader> findBySourceCategory(PaymentBatchHeader.SourceCategory sourceCategory);
    Optional<PaymentBatchHeader> findByOperationId(String operationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentBatchHeader> findById(String id);
}
