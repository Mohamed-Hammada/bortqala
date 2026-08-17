package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.CustomerReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerReceiptRepository extends JpaRepository<CustomerReceipt, String> {
    List<CustomerReceipt> findAllByOrderByReceiptDateDescCreatedAtDesc();

    Optional<CustomerReceipt> findByOperationId(String operationId);

    boolean existsByReceiptNumberIgnoreCase(String number);
}
