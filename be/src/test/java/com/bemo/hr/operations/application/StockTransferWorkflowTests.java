package com.bemo.hr.operations.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.operations.domain.StockTransferDiscrepancy;
import com.bemo.hr.operations.domain.StockTransferHeader;
import com.bemo.hr.operations.domain.StockTransferLine;
import com.bemo.hr.operations.infrastructure.CycleCountHeaderRepository;
import com.bemo.hr.operations.infrastructure.CycleCountLineRepository;
import com.bemo.hr.operations.infrastructure.StockTransferDiscrepancyRepository;
import com.bemo.hr.operations.infrastructure.StockTransferHeaderRepository;
import com.bemo.hr.operations.infrastructure.StockTransferLineRepository;
import com.bemo.hr.organization.domain.Branch;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StockTransferWorkflowTests {

    private StockTransferHeaderRepository transferHeaderRepository;
    private StockTransferLineRepository transferLineRepository;
    private CycleCountHeaderRepository cycleCountHeaderRepository;
    private CycleCountLineRepository cycleCountLineRepository;
    private OperationsService operationsService;
    private WarehouseInventoryService warehouseInventoryService;
    private WarehouseRepository warehouseRepository;
    private AuditService auditService;
    private StockTransferDiscrepancyRepository discrepancyRepository;
    private BranchRepository branchRepository;
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
        discrepancyRepository = mock(StockTransferDiscrepancyRepository.class);
        branchRepository = mock(BranchRepository.class);

        movementService = new InventoryMovementFullService(
                transferHeaderRepository,
                transferLineRepository,
                cycleCountHeaderRepository,
                cycleCountLineRepository,
                operationsService,
                warehouseInventoryService,
                warehouseRepository,
                auditService,
                discrepancyRepository,
                branchRepository
        );
    }

    @Test
    void createsBranchTransfer_setsSourceAndTargetBranchFromWarehouses() {
        Warehouse wSource = new Warehouse("b-cairo", "WH-CAI", "Cairo Central", "Cairo", true);
        Warehouse wTarget = new Warehouse("b-alex", "WH-ALX", "Alexandria Port", "Alex", true);

        when(warehouseRepository.findById("wh-1")).thenReturn(Optional.of(wSource));
        when(warehouseRepository.findById("wh-2")).thenReturn(Optional.of(wTarget));
        when(transferHeaderRepository.findByTransferNumberIgnoreCase("TRF-2026-001")).thenReturn(Optional.empty());
        when(transferHeaderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StockTransferHeader transfer = movementService.createTransfer("TRF-2026-001", "wh-1", "wh-2", LocalDate.of(2026, 9, 4));

        assertThat(transfer).isNotNull();
        assertThat(transfer.getSourceBranchId()).isEqualTo("b-cairo");
        assertThat(transfer.getTargetBranchId()).isEqualTo("b-alex");
        assertThat(transfer.getStatus()).isEqualTo(StockTransferHeader.Status.DRAFT);
    }

    @Test
    void dispatchTransfer_setsShippedStatusAndInTransitDetails() {
        StockTransferHeader transfer = new StockTransferHeader("TRF-001", "wh-1", "wh-2", "b-1", "b-2", LocalDate.now());
        StockTransferLine line = new StockTransferLine(transfer.getId(), "item-1", new BigDecimal("50.00"));

        when(transferHeaderRepository.findById(transfer.getId())).thenReturn(Optional.of(transfer));
        when(transferLineRepository.findByTransferId(transfer.getId())).thenReturn(List.of(line));
        when(warehouseInventoryService.getAvailableStock("wh-1", "item-1")).thenReturn(new BigDecimal("100.00"));
        when(transferHeaderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StockTransferHeader dispatched = movementService.dispatchTransfer(
                transfer.getId(),
                "Aramex Logistics",
                "Ahmed Mansour",
                "01012345678",
                "أ ب ج 1234",
                "WB-998877",
                "Express shipping with cooling container",
                "dispatcher-user"
        );

        assertThat(dispatched.getStatus()).isEqualTo(StockTransferHeader.Status.SHIPPED);
        assertThat(dispatched.getCarrierName()).isEqualTo("Aramex Logistics");
        assertThat(dispatched.getWaybillNumber()).isEqualTo("WB-998877");
        assertThat(dispatched.getDispatchedBy()).isEqualTo("dispatcher-user");
        assertThat(dispatched.getDispatchedAt()).isNotNull();

        verify(warehouseInventoryService).issueAvailableStock("wh-1", "item-1", new BigDecimal("50.00"));
    }

    @Test
    void receiveTransferWithInspection_recordsGoodStockAndCreatesDiscrepancies() {
        StockTransferHeader transfer = new StockTransferHeader("TRF-001", "wh-1", "wh-2", "b-1", "b-2", LocalDate.now());
        transfer.dispatch("DHL", "Sami", "0123456", "XYZ-99", "WB-123", null, "user1");

        StockTransferLine line = new StockTransferLine(transfer.getId(), "item-glass", new BigDecimal("100.00"));
        line.setShippedQuantity(new BigDecimal("100.00"));

        when(transferHeaderRepository.findById(transfer.getId())).thenReturn(Optional.of(transfer));
        when(transferLineRepository.findByTransferId(transfer.getId())).thenReturn(List.of(line));
        when(transferHeaderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(discrepancyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // 90 received good, 8 damaged during transit, 2 lost/broken
        var inspection = new InventoryMovementFullService.ReceiptInspectionLineInput(
                line.getId(),
                new BigDecimal("90.00"),
                new BigDecimal("8.00"),
                new BigDecimal("2.00"),
                "DISCREPANCY_DAMAGE",
                "Box damaged during road transport"
        );

        StockTransferHeader received = movementService.receiveTransferWithInspection(
                transfer.getId(),
                List.of(inspection),
                "Inspected by receiving manager",
                "receiving-manager"
        );

        assertThat(received.getStatus()).isEqualTo(StockTransferHeader.Status.RECEIVED);
        assertThat(received.isHasDiscrepancy()).isTrue();
        assertThat(received.getReceivedBy()).isEqualTo("receiving-manager");
        assertThat(received.getReceivedAt()).isNotNull();

        // Target warehouse must only receive the GOOD accepted quantity (90)
        verify(warehouseInventoryService).receiveAvailableStock("wh-2", "item-glass", new BigDecimal("90.00"));

        // Discrepancy record must be created and saved
        verify(discrepancyRepository).save(argThat(d ->
                d.getDamagedQuantity().compareTo(new BigDecimal("8.00")) == 0
                        && d.getLostQuantity().compareTo(new BigDecimal("2.00")) == 0
                        && d.getResolutionStatus() == StockTransferDiscrepancy.ResolutionStatus.PENDING
        ));
    }

    @Test
    void resolveDiscrepancy_updatesResolutionStatus() {
        StockTransferDiscrepancy disc = new StockTransferDiscrepancy(
                "trf-1", "line-1", "item-glass",
                new BigDecimal("100"), new BigDecimal("90"), new BigDecimal("8"), new BigDecimal("2"),
                StockTransferDiscrepancy.DiscrepancyType.DAMAGED, "manager"
        );

        when(discrepancyRepository.findById(disc.getId())).thenReturn(Optional.of(disc));
        when(discrepancyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StockTransferDiscrepancy resolved = movementService.resolveDiscrepancy(
                disc.getId(),
                "CLAIMED",
                "Insurance claim filed with logistics provider",
                "finance-officer"
        );

        assertThat(resolved.getResolutionStatus()).isEqualTo(StockTransferDiscrepancy.ResolutionStatus.CLAIMED);
        assertThat(resolved.getResolvedBy()).isEqualTo("finance-officer");
        assertThat(resolved.getResolutionNotes()).contains("Insurance claim filed");
    }
}
