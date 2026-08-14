package com.bemo.hr.inventory.application;

import com.bemo.hr.operations.application.WarehouseInventoryService;
import com.bemo.hr.operations.domain.StockReservation;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryServiceTests {

    private WarehouseRepository warehouseRepository;
    private WarehouseInventoryService warehouseInventoryService;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        warehouseRepository = mock(WarehouseRepository.class);
        warehouseInventoryService = mock(WarehouseInventoryService.class);
        inventoryService = new InventoryService(warehouseRepository, warehouseInventoryService);
    }

    @Test
    void createsWarehouseSuccessfully() {
        when(warehouseInventoryService.createWarehouse("branch-1", "WH-MAIN", "Main Warehouse", "Building A"))
                .thenReturn(new Warehouse("branch-1", "WH-MAIN", "Main Warehouse", "Building A", true));

        Warehouse wh = inventoryService.createWarehouse("branch-1", "WH-MAIN", "Main Warehouse", "Building A");

        assertThat(wh).isNotNull();
        assertThat(wh.getCode()).isEqualTo("WH-MAIN");
        assertThat(wh.getName()).isEqualTo("Main Warehouse");
    }

    @Test
    void reservesStockSuccessfully() {
        StockReservation expected = new StockReservation("RES-1", "SO", "so-123", "item-10", "wh-1",
                new BigDecimal("50.00"));
        when(warehouseInventoryService.reserveStock(anyString(), eq("SO"), eq("so-123"), eq("item-10"),
                eq("wh-1"), eq(new BigDecimal("50.00")))).thenReturn(expected);

        StockReservation reservation = inventoryService.reserveStock("SO", "so-123", "item-10", "wh-1", new BigDecimal("50.00"));

        assertThat(reservation).isNotNull();
        assertThat(reservation.getSourceId()).isEqualTo("so-123");
        assertThat(reservation.getReservedQuantity()).isEqualTo(new BigDecimal("50.00"));
        assertThat(reservation.getStatus()).isEqualTo(StockReservation.Status.ACTIVE);
    }
}
