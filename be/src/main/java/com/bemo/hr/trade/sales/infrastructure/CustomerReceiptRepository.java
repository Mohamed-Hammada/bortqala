package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.CustomerReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CustomerReceiptRepository extends JpaRepository<CustomerReceipt, String> {
    List<CustomerReceipt> findAllByOrderByReceiptDateDescCreatedAtDesc();

    Optional<CustomerReceipt> findByOperationId(String operationId);

    boolean existsByReceiptNumberIgnoreCase(String number);

    /**
     * 2026-09-07 remediation (Performance Review P-1): Executive Analytics used to call
     * {@code findAll()} and filter {@code receiptDate} in a Java stream to compute "today's
     * collections". Pushes the filter into SQL. Supported by {@code idx_customer_receipts_app_receipt_date}.
     */
    List<CustomerReceipt> findByReceiptDateBetween(LocalDate start, LocalDate end);
}
