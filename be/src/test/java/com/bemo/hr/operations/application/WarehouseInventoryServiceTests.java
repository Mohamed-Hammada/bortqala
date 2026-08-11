package com.bemo.hr.operations.application;

import com.bemo.hr.operations.domain.*;
import com.bemo.hr.operations.infrastructure.*;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WarehouseInventoryServiceTests {

    private WarehouseRepository warehouseRepository;
    private WarehouseBinRepository binRepository;
    private StockStatusBalanceRepository balanceRepository;
    private StockReservationRepository reservationRepository;
    private WarehouseInventoryService inventoryService;

    @BeforeEach
    void setUp() {
        warehouseRepository = mock(WarehouseRepository.class);
        binRepository = mock(WarehouseBinRepository.class);
        balanceRepository = mock(StockStatusBalanceRepository.class);
        reservationRepository = mock(StockReservationRepository.class);
        inventoryService = new WarehouseInventoryService(warehouseRepository, binRepository, balanceRepository, reservationRepository);
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

        StockReservation res = inventoryService.reserveStock("RES-001", "SALES_ORDER", "so-1", "item-10", "wh-1", new BigDecimal("20.00"));
        assertThat(res).isNotNull();
        assertThat(res.getStatus()).isEqualTo(StockReservation.Status.ACTIVE);
    }
}
