package com.bemo.hr.inventory.application;

import com.bemo.hr.inventory.domain.InventoryReservation;
import com.bemo.hr.inventory.infrastructure.InventoryReservationRepository;
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
    private InventoryReservationRepository reservationRepository;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        warehouseRepository = mock(WarehouseRepository.class);
        reservationRepository = mock(InventoryReservationRepository.class);
        inventoryService = new InventoryService(warehouseRepository, reservationRepository);
    }

    @Test
    void createsWarehouseSuccessfully() {
        when(warehouseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Warehouse wh = inventoryService.createWarehouse("branch-1", "WH-MAIN", "Main Warehouse", "Building A");

        assertThat(wh).isNotNull();
        assertThat(wh.getCode()).isEqualTo("WH-MAIN");
        assertThat(wh.getName()).isEqualTo("Main Warehouse");
    }

    @Test
    void reservesStockSuccessfully() {
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryReservation reservation = inventoryService.reserveStock("SO", "so-123", "item-10", "wh-1", new BigDecimal("50.00"));

        assertThat(reservation).isNotNull();
        assertThat(reservation.getSourceId()).isEqualTo("so-123");
        assertThat(reservation.getQuantity()).isEqualTo(new BigDecimal("50.00"));
        assertThat(reservation.getStatus()).isEqualTo(InventoryReservation.Status.ACTIVE);
    }
}
