package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.CustomerReceiptBankMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerReceiptBankMatchRepository extends JpaRepository<CustomerReceiptBankMatch, String> {
    Optional<CustomerReceiptBankMatch> findByReceiptId(String receiptId);
}
