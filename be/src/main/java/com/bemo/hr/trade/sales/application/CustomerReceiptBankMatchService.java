package com.bemo.hr.trade.sales.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.domain.CustomerReceiptBankMatch;
import com.bemo.hr.trade.sales.infrastructure.CustomerReceiptBankMatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CustomerReceiptBankMatchService {

    private final CustomerReceiptBankMatchRepository repository;

    public CustomerReceiptBankMatchService(CustomerReceiptBankMatchRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CustomerReceiptBankMatch matchReceipt(String receiptId, String bankTransactionId, BigDecimal matchedAmount) {
        CustomerReceiptBankMatch match = repository.findByReceiptId(receiptId)
                .orElseGet(() -> new CustomerReceiptBankMatch(receiptId, bankTransactionId, matchedAmount));
        return repository.save(match);
    }

    @Transactional(readOnly = true)
    public CustomerReceiptBankMatch getMatchForReceipt(String receiptId) {
        return repository.findByReceiptId(receiptId)
                .orElseThrow(() -> new BusinessRuleException("Customer receipt bank match not found", "RECEIPT_BANK_MATCH_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
