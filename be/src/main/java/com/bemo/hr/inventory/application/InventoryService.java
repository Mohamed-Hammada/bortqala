package com.bemo.hr.inventory.application;

import com.bemo.hr.operations.application.WarehouseInventoryService;
import com.bemo.hr.operations.domain.StockReservation;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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
        return warehouseInventoryService.createWarehouse(branchId, code, name, location);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @Transactional
    public StockReservation reserveStock(String sourceType, String sourceId, String itemId, String warehouseId, BigDecimal quantity) {
        String reservationNumber = "API-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return warehouseInventoryService.reserveStock(
                reservationNumber, sourceType, sourceId, itemId, warehouseId, quantity);
    }

    @Transactional
    public void releaseReservation(String reservationId) {
        warehouseInventoryService.cancelReservation(reservationId);
    }
}
