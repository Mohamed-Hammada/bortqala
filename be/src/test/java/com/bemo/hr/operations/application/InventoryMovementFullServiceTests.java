package com.bemo.hr.operations.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.operations.domain.CycleCountHeader;
import com.bemo.hr.operations.domain.CycleCountLine;
import com.bemo.hr.operations.domain.StockTransferHeader;
import com.bemo.hr.operations.domain.StockTransferLine;
import com.bemo.hr.operations.infrastructure.CycleCountHeaderRepository;
import com.bemo.hr.operations.infrastructure.CycleCountLineRepository;
import com.bemo.hr.operations.infrastructure.StockTransferHeaderRepository;
import com.bemo.hr.operations.infrastructure.StockTransferLineRepository;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
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
    private WarehouseInventoryService warehouseInventoryService;
    private WarehouseRepository warehouseRepository;
    private AuditService auditService;
    private InventoryMovementFullService movementService;

    @BeforeEach
    void setUp() {
        transferHeaderRepository = mock(StockTransferHeaderRepository.class);
        transferLineRepository = mock(StockTransferLineRepository.class);
        cycleCountHeaderRepository = mock(CycleCountHeaderRepository.class);
        cycleCountLineRepository = mock(CycleCountLineRepository.class);
        operationsService = mock(OperationsService.class);
        warehouseInventoryService = mock(WarehouseInventoryService.class);
        warehouseRepository = mock(WarehouseRepository.class);
        auditService = mock(AuditService.class);
        movementService = new InventoryMovementFullService(transferHeaderRepository, transferLineRepository,
                cycleCountHeaderRepository, cycleCountLineRepository, operationsService,
                warehouseInventoryService, warehouseRepository, auditService);
    }

    @Test
    void createsShipsAndReceivesStockTransferAndCycleCountSuccessfully() {
        StockTransferHeader transfer = new StockTransferHeader("TRF-100", "wh-1", "wh-2", LocalDate.of(2026, 3, 1));
        when(transferHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transferHeaderRepository.findById("trf-1")).thenReturn(Optional.of(transfer));

        StockTransferLine transferLine = new StockTransferLine("trf-1", "item-10", new BigDecimal("4.00"));
        when(transferLineRepository.findByTransferId("trf-1")).thenReturn(java.util.List.of(transferLine));
        when(warehouseInventoryService.getAvailableStock("wh-1", "item-10")).thenReturn(new BigDecimal("10.00"));

        movementService.shipTransfer("trf-1", "inventory-manager");
        assertThat(transfer.getStatus()).isEqualTo(StockTransferHeader.Status.SHIPPED);
        verify(warehouseInventoryService).issueAvailableStock("wh-1", "item-10", new BigDecimal("4.00"));

        movementService.receiveTransfer("trf-1", "inventory-manager");
        assertThat(transfer.getStatus()).isEqualTo(StockTransferHeader.Status.RECEIVED);
        verify(warehouseInventoryService).receiveAvailableStock("wh-2", "item-10", new BigDecimal("4.00"));

        CycleCountHeader count = new CycleCountHeader("CC-001", "wh-1", LocalDate.of(2026, 3, 5));
        count.start();
        when(cycleCountHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cycleCountHeaderRepository.findById("cc-1")).thenReturn(Optional.of(count));
        when(cycleCountLineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(warehouseInventoryService.getPhysicalStock("wh-1", "item-10")).thenReturn(new BigDecimal("10.00"));

        CycleCountLine line = movementService.addCycleCountLine("cc-1", "item-10", new BigDecimal("999.00"), new BigDecimal("12.00"));
        assertThat(line.getVarianceQuantity()).isEqualByComparingTo(new BigDecimal("2.00"));

        when(cycleCountLineRepository.findByCountId("cc-1")).thenReturn(java.util.List.of(line));
        movementService.adjustCycleCount("cc-1", "inventory-manager");
        assertThat(count.getStatus()).isEqualTo(CycleCountHeader.Status.ADJUSTED);
        verify(operationsService).createStockAdjustment(any(), eq("inventory-manager"));
        verify(warehouseInventoryService).adjustAvailableStock("wh-1", "item-10", new BigDecimal("2.00"));
    }

    @Test
    void rejectsShippingWhenAnyLineExceedsSourceWarehouseStockWithoutMovingAnything() {
        StockTransferHeader transfer = new StockTransferHeader("TRF-LOW", "wh-1", "wh-2", LocalDate.of(2026, 3, 1));
        StockTransferLine line = new StockTransferLine("trf-low", "item-10", new BigDecimal("11.00"));
        when(transferHeaderRepository.findById("trf-low")).thenReturn(Optional.of(transfer));
        when(transferLineRepository.findByTransferId("trf-low")).thenReturn(java.util.List.of(line));
        when(warehouseInventoryService.getAvailableStock("wh-1", "item-10")).thenReturn(new BigDecimal("10.00"));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> movementService.shipTransfer("trf-low", "inventory-manager"))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class)
                .satisfies(error -> assertThat(((com.bemo.hr.shared.domain.BusinessRuleException) error).getCode())
                        .isEqualTo("TRANSFER_SOURCE_STOCK_INSUFFICIENT"));
        assertThat(transfer.getStatus()).isEqualTo(StockTransferHeader.Status.DRAFT);
        verify(warehouseInventoryService, never()).issueAvailableStock(anyString(), anyString(), any());
    }

    @Test
    void shipAndReceiveReplaysDoNotMoveStockTwice() {
        StockTransferHeader transfer = new StockTransferHeader("TRF-IDEMP", "wh-1", "wh-2", LocalDate.of(2026, 3, 1));
        StockTransferLine line = new StockTransferLine("trf-idemp", "item-10", new BigDecimal("2.00"));
        when(transferHeaderRepository.findById("trf-idemp")).thenReturn(Optional.of(transfer));
        when(transferLineRepository.findByTransferId("trf-idemp")).thenReturn(java.util.List.of(line));
        when(warehouseInventoryService.getAvailableStock("wh-1", "item-10")).thenReturn(new BigDecimal("10.00"));
        when(transferHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        movementService.shipTransfer("trf-idemp", "manager");
        movementService.shipTransfer("trf-idemp", "manager");
        movementService.receiveTransfer("trf-idemp", "manager");
        movementService.receiveTransfer("trf-idemp", "manager");

        verify(warehouseInventoryService, times(1)).issueAvailableStock("wh-1", "item-10", new BigDecimal("2.00"));
        verify(warehouseInventoryService, times(1)).receiveAvailableStock("wh-2", "item-10", new BigDecimal("2.00"));
    }

    @Test
    void cycleCountPostsNegativeWarehouseVariance() {
        CycleCountHeader count = new CycleCountHeader("CC-NEG", "wh-1", LocalDate.of(2026, 3, 6));
        count.start();
        when(cycleCountHeaderRepository.findById("cc-neg")).thenReturn(Optional.of(count));
        when(cycleCountHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cycleCountLineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(warehouseInventoryService.getPhysicalStock("wh-1", "item-10")).thenReturn(new BigDecimal("10"));
        CycleCountLine line = movementService.addCycleCountLine("cc-neg", "item-10", null, new BigDecimal("7"));
        when(cycleCountLineRepository.findByCountId("cc-neg")).thenReturn(java.util.List.of(line));

        movementService.adjustCycleCount("cc-neg", "manager");

        assertThat(line.getVarianceQuantity()).isEqualByComparingTo("-3");
        verify(warehouseInventoryService).adjustAvailableStock("wh-1", "item-10", new BigDecimal("-3"));
    }
}
