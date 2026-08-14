package com.bemo.hr.operations.application;

import com.bemo.hr.operations.domain.*;
import com.bemo.hr.operations.infrastructure.*;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.organization.domain.Branch;
import com.bemo.hr.operations.InventoryItem;
import com.bemo.hr.operations.InventoryItemRepository;
import com.bemo.hr.audit.application.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WarehouseInventoryServiceTests {

    private WarehouseRepository warehouseRepository;
    private WarehouseBinRepository binRepository;
    private StockStatusBalanceRepository balanceRepository;
    private StockReservationRepository reservationRepository;
    private WarehouseInventoryService inventoryService;
    private AuditService auditService;
    private BranchRepository branchRepository;
    private InventoryItemRepository inventoryItemRepository;

    @BeforeEach
    void setUp() {
        warehouseRepository = mock(WarehouseRepository.class);
        binRepository = mock(WarehouseBinRepository.class);
        balanceRepository = mock(StockStatusBalanceRepository.class);
        reservationRepository = mock(StockReservationRepository.class);
        auditService = mock(AuditService.class);
        branchRepository = mock(BranchRepository.class);
        inventoryItemRepository = mock(InventoryItemRepository.class);
        inventoryService = new WarehouseInventoryService(warehouseRepository, binRepository, balanceRepository,
                reservationRepository, auditService, branchRepository, inventoryItemRepository);
        Warehouse active = mock(Warehouse.class);
        when(active.isActive()).thenReturn(true);
        when(warehouseRepository.findById("wh-1")).thenReturn(java.util.Optional.of(active));
        Branch branch = mock(Branch.class);
        when(branch.isActive()).thenReturn(true);
        when(branchRepository.findById("branch-1")).thenReturn(java.util.Optional.of(branch));
        InventoryItem item = mock(InventoryItem.class);
        when(item.isActive()).thenReturn(true);
        when(inventoryItemRepository.findById(anyString())).thenReturn(java.util.Optional.of(item));
    }

    @Test
    void createsWarehouseBinAndStockReservationSuccessfully() {
        when(warehouseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(binRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Warehouse wh = inventoryService.createWarehouse("branch-1", "WH-MAIN", "Main Warehouse", "Cairo");
        assertThat(wh).isNotNull();
        assertThat(wh.getCode()).isEqualTo("WH-MAIN");

        WarehouseBin bin = inventoryService.createBin("wh-1", "BIN-A1", "Aisle 1", "Rack 2", "Shelf 3");
        assertThat(bin).isNotNull();
        assertThat(bin.getBinCode()).isEqualTo("BIN-A1");

        StockStatusBalance balance = new StockStatusBalance("wh-1", "bin-1", "item-10", StockStatusBalance.Status.AVAILABLE, new BigDecimal("100.00"));
        when(balanceRepository.findByWarehouseIdAndItemId("wh-1", "item-10")).thenReturn(List.of(balance));
        when(balanceRepository.findByWarehouseIdAndItemIdForUpdate("wh-1", "item-10")).thenReturn(List.of(balance));

        StockReservation res = inventoryService.reserveStock("RES-001", "SALES_ORDER", "so-1", "item-10", "wh-1", new BigDecimal("20.00"));
        assertThat(res).isNotNull();
        assertThat(res.getStatus()).isEqualTo(StockReservation.Status.ACTIVE);
    }

    @Test
    void movesStatusWithoutChangingPhysicalQuantityAndRecordsAudit() {
        StockStatusBalance available = new StockStatusBalance("wh-1", "", "item-1", StockStatusBalance.Status.AVAILABLE, new BigDecimal("10"));
        when(balanceRepository.findByWarehouseIdAndItemIdForUpdate("wh-1", "item-1")).thenReturn(List.of(available));
        when(balanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.moveStatus("wh-1", "", "item-1", StockStatusBalance.Status.AVAILABLE,
                StockStatusBalance.Status.QUARANTINE, new BigDecimal("3"), "manager");

        assertThat(available.getQuantity()).isEqualByComparingTo("7");
        var saved = org.mockito.ArgumentCaptor.forClass(StockStatusBalance.class);
        verify(balanceRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().stream().map(StockStatusBalance::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("10");
        verify(auditService).record(eq("STOCK_STATUS_CHANGED"), eq("STOCK_STATUS_BALANCE"), any(), eq("manager"), any(), isNull());
    }

    @Test
    void rejectsNegativeStatusBalanceAndCrossWarehouseBin() {
        StockStatusBalance balance = new StockStatusBalance("wh-1", "", "item-1", StockStatusBalance.Status.AVAILABLE, BigDecimal.ONE);
        assertThatThrownBy(() -> balance.adjustQuantity(new BigDecimal("-2")))
                .isInstanceOf(IllegalArgumentException.class);

        WarehouseBin wrongBin = new WarehouseBin("wh-2", "B-1", null, null, null);
        when(binRepository.findById(wrongBin.getId())).thenReturn(java.util.Optional.of(wrongBin));
        assertThatThrownBy(() -> inventoryService.moveStatus("wh-1", wrongBin.getId(), "item-1",
                StockStatusBalance.Status.AVAILABLE, StockStatusBalance.Status.BLOCKED, BigDecimal.ONE, "manager"))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class)
                .satisfies(error -> assertThat(((com.bemo.hr.shared.domain.BusinessRuleException) error).getCode())
                        .isEqualTo("WAREHOUSE_BIN_MISMATCH"));
    }

    @Test
    void cancellationAndExpiryReleaseReservationFromAvailableToPromise() {
        StockStatusBalance available = new StockStatusBalance("wh-1", "", "item-1", StockStatusBalance.Status.AVAILABLE, new BigDecimal("10"));
        StockReservation active = new StockReservation("R-1", "ORDER", "o-1", "item-1", "wh-1", new BigDecimal("4"));
        when(balanceRepository.findByWarehouseIdAndItemId("wh-1", "item-1")).thenReturn(List.of(available));
        when(reservationRepository.findByWarehouseIdAndItemIdAndStatus("wh-1", "item-1", StockReservation.Status.ACTIVE))
                .thenReturn(List.of(active));
        when(reservationRepository.findById(active.getId())).thenReturn(java.util.Optional.of(active));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(inventoryService.getAvailableStock("wh-1", "item-1")).isEqualByComparingTo("6");

        inventoryService.expireReservation(active.getId());
        when(reservationRepository.findByWarehouseIdAndItemIdAndStatus("wh-1", "item-1", StockReservation.Status.ACTIVE))
                .thenReturn(List.of());
        assertThat(inventoryService.getAvailableStock("wh-1", "item-1")).isEqualByComparingTo("10");
        assertThat(active.getStatus()).isEqualTo(StockReservation.Status.EXPIRED);
    }

    @Test
    void reservationRejectsMissingItemNonPositiveQuantityAndInsufficientStock() {
        when(inventoryItemRepository.findById("missing-item")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> inventoryService.reserveStock("R-1", "ORDER", "o-1",
                "missing-item", "wh-1", BigDecimal.ONE))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class)
                .extracting("code").isEqualTo("WAREHOUSE_ITEM_ACTIVE_REQUIRED");
        assertThatThrownBy(() -> inventoryService.reserveStock("R-2", "ORDER", "o-2",
                "item-1", "wh-1", BigDecimal.ZERO))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class)
                .extracting("code").isEqualTo("WAREHOUSE_QUANTITY_POSITIVE");

        StockStatusBalance balance = new StockStatusBalance("wh-1", "", "item-1",
                StockStatusBalance.Status.AVAILABLE, new BigDecimal("2"));
        when(balanceRepository.findByWarehouseIdAndItemIdForUpdate("wh-1", "item-1"))
                .thenReturn(List.of(balance));
        assertThatThrownBy(() -> inventoryService.reserveStock("R-3", "ORDER", "o-3",
                "item-1", "wh-1", new BigDecimal("3")))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class)
                .extracting("code").isEqualTo("INSUFFICIENT_STOCK_RESERVATION");
    }

    @Test
    void warehouseCreationRejectsMissingBranchInsteadOfUsingAFakeDefault() {
        assertThatThrownBy(() -> inventoryService.createWarehouse(null, "WH", "Warehouse", null))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class)
                .extracting("code").isEqualTo("WAREHOUSE_BRANCH_REQUIRED");
    }
}
