package com.bemo.hr.trade.sales.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.domain.CustomerReceiptBankMatch;
import com.bemo.hr.trade.sales.infrastructure.CustomerReceiptBankMatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class CustomerReceiptBankMatchService {

    private final CustomerReceiptBankMatchRepository repository;

    public CustomerReceiptBankMatchService(CustomerReceiptBankMatchRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CustomerReceiptBankMatch matchReceipt(String receiptId, String bankTransactionId, BigDecimal matchedAmount) {
        log.debug("matchReceipt called with receiptId={}, bankTransactionId={}, matchedAmount={}", receiptId, bankTransactionId, matchedAmount);
        CustomerReceiptBankMatch match = repository.findByReceiptId(receiptId)
                .orElseGet(() -> new CustomerReceiptBankMatch(receiptId, bankTransactionId, matchedAmount));
        CustomerReceiptBankMatch saved = repository.save(match);
        log.info("ReceiptBankMatch {} for receipt {} matched successfully", saved.getId(), receiptId);
        return saved;
    }

    @Transactional(readOnly = true)
    public CustomerReceiptBankMatch getMatchForReceipt(String receiptId) {
        log.debug("getMatchForReceipt called with receiptId={}", receiptId);
        return repository.findByReceiptId(receiptId)
                .orElseThrow(() -> {
                    log.warn("Customer receipt bank match not found for receiptId={}", receiptId);
                    return new BusinessRuleException("Customer receipt bank match not found", "RECEIPT_BANK_MATCH_NOT_FOUND", HttpStatus.NOT_FOUND);
                });
    }
}
