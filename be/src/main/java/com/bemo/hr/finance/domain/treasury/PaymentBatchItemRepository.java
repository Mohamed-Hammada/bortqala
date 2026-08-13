package com.bemo.hr.finance.domain.treasury;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentBatchItemRepository extends JpaRepository<PaymentBatchItem, String> {
    List<PaymentBatchItem> findByBatchId(String batchId);
    boolean existsByBatchIdAndDocumentId(String batchId, String documentId);
}
