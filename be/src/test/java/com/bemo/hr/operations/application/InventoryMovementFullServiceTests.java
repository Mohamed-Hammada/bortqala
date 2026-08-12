package com.bemo.hr.operations.application;

import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.operations.domain.*;
import com.bemo.hr.operations.infrastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryMovementFullServiceTests {

    private StockTransferHeaderRepository transferHeaderRepository;
    private StockTransferLineRepository transferLineRepository;
    private CycleCountHeaderRepository cycleCountHeaderRepository;
    private CycleCountLineRepository cycleCountLineRepository;
    private OperationsService operationsService;
    private InventoryMovementFullService movementService;

    @BeforeEach
    void setUp() {
        transferHeaderRepository = mock(StockTransferHeaderRepository.class);
        transferLineRepository = mock(StockTransferLineRepository.class);
        cycleCountHeaderRepository = mock(CycleCountHeaderRepository.class);
        cycleCountLineRepository = mock(CycleCountLineRepository.class);
        operationsService = mock(OperationsService.class);
        movementService = new InventoryMovementFullService(transferHeaderRepository, transferLineRepository, cycleCountHeaderRepository, cycleCountLineRepository, operationsService);
    }

    @Test
    void createsShipsAndReceivesStockTransferAndCycleCountSuccessfully() {
        StockTransferHeader transfer = new StockTransferHeader("TRF-100", "wh-1", "wh-2", LocalDate.of(2026, 3, 1));
        when(transferHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transferHeaderRepository.findById("trf-1")).thenReturn(Optional.of(transfer));

        movementService.shipTransfer("trf-1");
        assertThat(transfer.getStatus()).isEqualTo(StockTransferHeader.Status.SHIPPED);

        movementService.receiveTransfer("trf-1");
        assertThat(transfer.getStatus()).isEqualTo(StockTransferHeader.Status.RECEIVED);

        CycleCountHeader count = new CycleCountHeader("CC-001", "wh-1", LocalDate.of(2026, 3, 5));
        count.start();
        when(cycleCountHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cycleCountHeaderRepository.findById("cc-1")).thenReturn(Optional.of(count));
        when(cycleCountLineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(operationsService.stockBalance("item-10")).thenReturn(new BigDecimal("10.00"));

        CycleCountLine line = movementService.addCycleCountLine("cc-1", "item-10", new BigDecimal("999.00"), new BigDecimal("12.00"));
        assertThat(line.getVarianceQuantity()).isEqualByComparingTo(new BigDecimal("2.00"));

        when(cycleCountLineRepository.findByCountId("cc-1")).thenReturn(java.util.List.of(line));
        movementService.adjustCycleCount("cc-1", "inventory-manager");
        assertThat(count.getStatus()).isEqualTo(CycleCountHeader.Status.ADJUSTED);
        verify(operationsService).createStockAdjustment(any(), eq("inventory-manager"));
    }
}
