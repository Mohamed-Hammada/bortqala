package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.BomSnapshot;
import com.bemo.hr.manufacturing.production.domain.ProductionOrder;
import com.bemo.hr.manufacturing.production.domain.ProductionVarianceClose;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionOrderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionVarianceCloseRepository;
import com.bemo.hr.operations.OperationsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ManufacturingVarianceCloseServiceTests {

    private ProductionVarianceCloseRepository repository;
    private ProductionOrderRepository productionOrderRepository;
    private BomSnapshotService bomSnapshotService;
    private OperationsService operationsService;
    private ManufacturingVarianceCloseService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductionVarianceCloseRepository.class);
        productionOrderRepository = mock(ProductionOrderRepository.class);
        bomSnapshotService = mock(BomSnapshotService.class);
        operationsService = mock(OperationsService.class);
        service = new ManufacturingVarianceCloseService(repository, productionOrderRepository,
                bomSnapshotService, operationsService);
    }

    @Test
    void calculatesAndClosesVarianceSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ProductionOrder order = new ProductionOrder("WO-200", "bom-1", "fg-1", "R1",
                BigDecimal.ONE, java.time.LocalDate.of(2026, 8, 1), null);
        when(productionOrderRepository.findById("wo-200")).thenReturn(Optional.of(order));
        when(bomSnapshotService.getSnapshotsForProductionOrder("wo-200")).thenReturn(java.util.List.of(
                new BomSnapshot("wo-200", "bom-1", 1, "rm-1", new BigDecimal("100"), new BigDecimal("100"))));
        when(operationsService.productionIssueCost("WO-200", "rm-1")).thenReturn(new BigDecimal("10500.00"));

        ProductionVarianceClose close = service.calculateAndCloseVariance("wo-200");
        assertThat(close).isNotNull();
        assertThat(close.getStandardCost()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(close.getActualCost()).isEqualByComparingTo(new BigDecimal("10500.00"));
        assertThat(close.getVarianceCost()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(close.getStatus()).isEqualTo(ProductionVarianceClose.Status.CLOSED);

        when(repository.findByWorkOrderId("wo-200")).thenReturn(Optional.of(close));
        ProductionVarianceClose fetched = service.getVarianceClose("wo-200");
        assertThat(fetched).isNotNull();
    }
}
