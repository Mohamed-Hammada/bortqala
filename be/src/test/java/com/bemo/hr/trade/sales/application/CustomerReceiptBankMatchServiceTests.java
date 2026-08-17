package com.bemo.hr.trade.sales.application;

import com.bemo.hr.trade.sales.domain.CustomerReceiptBankMatch;
import com.bemo.hr.trade.sales.infrastructure.CustomerReceiptBankMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerReceiptBankMatchServiceTests {

    private CustomerReceiptBankMatchRepository repository;
    private CustomerReceiptBankMatchService service;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerReceiptBankMatchRepository.class);
        service = new CustomerReceiptBankMatchService(repository);
    }

    @Test
    void matchesCustomerReceiptWithBankTransactionSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerReceiptBankMatch match = service.matchReceipt("rec-50", "bank-tx-99", new BigDecimal("4500.00"));
        assertThat(match).isNotNull();
        assertThat(match.getReceiptId()).isEqualTo("rec-50");
        assertThat(match.getBankTransactionId()).isEqualTo("bank-tx-99");
        assertThat(match.getStatus()).isEqualTo(CustomerReceiptBankMatch.Status.MATCHED);

        when(repository.findByReceiptId("rec-50")).thenReturn(Optional.of(match));
        assertThat(service.getMatchForReceipt("rec-50")).isNotNull();
    }
}
