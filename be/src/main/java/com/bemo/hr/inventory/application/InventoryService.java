package com.bemo.hr.inventory.application;

import com.bemo.hr.operations.application.WarehouseInventoryService;
import com.bemo.hr.operations.domain.StockReservation;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class InventoryService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseInventoryService warehouseInventoryService;

    public InventoryService(WarehouseRepository warehouseRepository,
                            WarehouseInventoryService warehouseInventoryService) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseInventoryService = warehouseInventoryService;
    }

    @Transactional
    public Warehouse createWarehouse(String branchId, String code, String name, String location) {
        log.debug("createWarehouse called with branchId={}, code={}, name={}", branchId, code, name);
        Warehouse warehouse = warehouseInventoryService.createWarehouse(branchId, code, name, location);
        log.info("Warehouse {} created successfully", warehouse.getId());
        return warehouse;
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehouses() {
        log.debug("getAllWarehouses called");
        return warehouseRepository.findAll();
    }

    @Transactional
    public StockReservation reserveStock(String sourceType, String sourceId, String itemId, String warehouseId, BigDecimal quantity) {
        log.debug("reserveStock called with sourceType={}, sourceId={}, itemId={}, warehouseId={}, quantity={}", sourceType, sourceId, itemId, warehouseId, quantity);
        String reservationNumber = "API-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        StockReservation reservation = warehouseInventoryService.reserveStock(
                reservationNumber, sourceType, sourceId, itemId, warehouseId, quantity);
        log.info("Stock reservation {} created successfully", reservation.getId());
        return reservation;
    }

    @Transactional
    public void releaseReservation(String reservationId) {
        log.debug("releaseReservation called with reservationId={}", reservationId);
        warehouseInventoryService.cancelReservation(reservationId);
        log.info("Stock reservation {} released successfully", reservationId);
    }
}
