package com.bemo.hr.trade.sales.application;

import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerInvoiceServiceTests {

    private CustomerInvoiceRepository repository;
    private CustomerInvoiceService service;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerInvoiceRepository.class);
        service = new CustomerInvoiceService(repository);
    }

    @Test
    void createsInvoiceFromDeliveredQuantityAndCalculatesCogsSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerInvoice invoice = service.createInvoiceFromDelivery("so-100", new BigDecimal("50.0000"), new BigDecimal("200.00"), new BigDecimal("120.00"));
        assertThat(invoice).isNotNull();
        assertThat(invoice.getDeliveredQuantity()).isEqualByComparingTo(new BigDecimal("50.0000"));
        assertThat(invoice.getInvoicedAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(invoice.getCogsAmount()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(invoice.getStatus()).isEqualTo(CustomerInvoice.Status.POSTED);

        when(repository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        assertThat(service.getInvoice(invoice.getId())).isNotNull();
    }
}
