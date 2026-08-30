package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.ProductionReceipt;
import com.bemo.hr.manufacturing.production.domain.RoutingHeader;
import com.bemo.hr.manufacturing.production.domain.WorkCenter;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionReceiptRepository;
import com.bemo.hr.manufacturing.production.infrastructure.RoutingHeaderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.WorkCenterRepository;
import com.bemo.hr.operations.OperationsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManufacturingExecutionServiceTests {

    private WorkCenterRepository workCenterRepository;
    private RoutingHeaderRepository routingHeaderRepository;
    private ProductionReceiptRepository receiptRepository;
    private OperationsService operationsService;
    private ManufacturingExecutionService manufacturingExecutionService;

    @BeforeEach
    void setUp() {
        workCenterRepository = mock(WorkCenterRepository.class);
        routingHeaderRepository = mock(RoutingHeaderRepository.class);
        receiptRepository = mock(ProductionReceiptRepository.class);
        operationsService = mock(OperationsService.class);
        manufacturingExecutionService = new ManufacturingExecutionService(workCenterRepository, routingHeaderRepository, receiptRepository, operationsService);
    }

    @Test
    void createsWorkCenterRoutingAndProductionReceiptSuccessfully() {
        when(workCenterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(routingHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(receiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkCenter wc = manufacturingExecutionService.createWorkCenter("WC-CUT", "Cutting Center", new BigDecimal("50.00"), new BigDecimal("8.00"));
        assertThat(wc).isNotNull();
        assertThat(wc.getCode()).isEqualTo("WC-CUT");

        RoutingHeader routing = manufacturingExecutionService.createRouting("ROUT-01", "Main Assembly Routing", "item-99");
        assertThat(routing).isNotNull();
        assertThat(routing.getRoutingCode()).isEqualTo("ROUT-01");

        ProductionReceipt receipt = manufacturingExecutionService.recordReceipt("PR-001", "po-55", "item-99", new BigDecimal("10.00"), LocalDate.of(2026, 3, 1), "wh-main");
        assertThat(receipt).isNotNull();
        assertThat(receipt.getReceivedQuantity()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void listsWorkCentersAndRoutings() {
        when(workCenterRepository.findAll()).thenReturn(java.util.List.of(
                new WorkCenter("WC-01", "Assembly", new BigDecimal("100.00"), new BigDecimal("8.00"))
        ));
        when(routingHeaderRepository.findAll()).thenReturn(java.util.List.of(
                new RoutingHeader("RT-01", "Assembly Line 1", "item-1")
        ));

        assertThat(manufacturingExecutionService.listWorkCenters()).hasSize(1);
        assertThat(manufacturingExecutionService.listRoutings()).hasSize(1);
    }
}
