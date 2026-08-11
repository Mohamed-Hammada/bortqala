package com.bemo.hr.trade.sales.application;

import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.trade.sales.domain.CustomerReturnHeader;
import com.bemo.hr.trade.sales.domain.SalesDeliveryHeader;
import com.bemo.hr.trade.sales.domain.SalesOrderLine;
import com.bemo.hr.trade.sales.infrastructure.CustomerReturnHeaderRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesDeliveryHeaderRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesOrderLineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SalesOrderFullServiceTests {

    private SalesOrderLineRepository salesOrderLineRepository;
    private SalesDeliveryHeaderRepository deliveryHeaderRepository;
    private CustomerReturnHeaderRepository returnHeaderRepository;
    private OperationsService operationsService;
    private SalesOrderFullService salesOrderFullService;

    @BeforeEach
    void setUp() {
        salesOrderLineRepository = mock(SalesOrderLineRepository.class);
        deliveryHeaderRepository = mock(SalesDeliveryHeaderRepository.class);
        returnHeaderRepository = mock(CustomerReturnHeaderRepository.class);
        operationsService = mock(OperationsService.class);
        salesOrderFullService = new SalesOrderFullService(salesOrderLineRepository, deliveryHeaderRepository, returnHeaderRepository, operationsService);
    }

    @Test
    void addsLineCreatesDeliveryAndCustomerReturnSuccessfully() {
        when(salesOrderLineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(returnHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SalesOrderLine line = salesOrderFullService.addSalesOrderLine("so-100", "item-5", "Laptop", new BigDecimal("2.00"), new BigDecimal("1000.00"), new BigDecimal("10.00"));
        assertThat(line).isNotNull();
        assertThat(line.getNetPrice()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(line.getLineTotal()).isEqualByComparingTo(new BigDecimal("1800.00"));

        SalesDeliveryHeader delivery = salesOrderFullService.createDelivery("DEL-001", "so-100", "cust-1", LocalDate.of(2026, 2, 1));
        assertThat(delivery.getStatus()).isEqualTo(SalesDeliveryHeader.Status.DELIVERED);

        CustomerReturnHeader returnHeader = salesOrderFullService.createCustomerReturn("RET-001", "so-100", "cust-1", LocalDate.of(2026, 2, 5), "Damaged packaging");
        assertThat(returnHeader.getStatus()).isEqualTo(CustomerReturnHeader.Status.RECEIVED);
    }
}
