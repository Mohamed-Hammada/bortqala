package com.bemo.hr.operations.api;

import com.bemo.hr.operations.application.WarehouseInventoryService;
import com.bemo.hr.operations.domain.StockReservation;
import com.bemo.hr.operations.domain.WarehouseBin;
import com.bemo.hr.organization.domain.Warehouse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/operations")
public class WarehouseInventoryController {

    private final WarehouseInventoryService inventoryService;

    public WarehouseInventoryController(WarehouseInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public record CreateWarehousePayload(String branchId, String code, String name, String location) {}
    public record CreateBinPayload(String binCode, String aisle, String rack, String shelf) {}
    public record ReserveStockPayload(String reservationNumber, String sourceType, String sourceId, String itemId, String warehouseId, BigDecimal quantity) {}

    @PostMapping("/warehouses")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER')")
    public Warehouse createWarehouse(@RequestBody CreateWarehousePayload payload) {
        return inventoryService.createWarehouse(payload.branchId(), payload.code(), payload.name(), payload.location());
    }

    @PostMapping("/warehouses/{id}/bins")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER')")
    public WarehouseBin createBin(@PathVariable String id, @RequestBody CreateBinPayload payload) {
        return inventoryService.createBin(id, payload.binCode(), payload.aisle(), payload.rack(), payload.shelf());
    }

    @PostMapping("/reservations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER', 'SALES_MANAGER')")
    public StockReservation reserveStock(@RequestBody ReserveStockPayload payload) {
        return inventoryService.reserveStock(payload.reservationNumber(), payload.sourceType(), payload.sourceId(), payload.itemId(), payload.warehouseId(), payload.quantity());
    }

    @PostMapping("/reservations/{id}/fulfill")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER')")
    public StockReservation fulfillReservation(@PathVariable String id) {
        return inventoryService.fulfillReservation(id);
    }

    @PostMapping("/reservations/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER')")
    public StockReservation cancelReservation(@PathVariable String id) {
        return inventoryService.cancelReservation(id);
    }

    @GetMapping("/warehouses/{id}/items/{itemId}/available")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER', 'SALES_MANAGER', 'VIEWER')")
    public BigDecimal getAvailableStock(@PathVariable String id, @PathVariable String itemId) {
        return inventoryService.getAvailableStock(id, itemId);
    }
}
