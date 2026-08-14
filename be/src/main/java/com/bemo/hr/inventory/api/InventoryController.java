package com.bemo.hr.inventory.api;

import com.bemo.hr.inventory.application.InventoryService;
import com.bemo.hr.operations.domain.StockReservation;
import com.bemo.hr.organization.domain.Warehouse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public record CreateWarehouseRequest(String branchId, String code, String name, String location) {}
    public record ReserveStockRequest(String sourceType, String sourceId, String itemId, String warehouseId, BigDecimal quantity) {}

    @GetMapping("/warehouses")
    @PreAuthorize("hasAuthority('P_INVENTORY_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public List<Warehouse> getWarehouses() {
        return inventoryService.getAllWarehouses();
    }

    @PostMapping("/warehouses")
    @PreAuthorize("hasAuthority('P_INVENTORY_MANAGE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public Warehouse createWarehouse(@RequestBody CreateWarehouseRequest request) {
        return inventoryService.createWarehouse(request.branchId(), request.code(), request.name(), request.location());
    }

    @PostMapping("/reservations")
    @PreAuthorize("hasAuthority('P_INVENTORY_MANAGE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public StockReservation reserveStock(@RequestBody ReserveStockRequest request) {
        return inventoryService.reserveStock(request.sourceType(), request.sourceId(), request.itemId(), request.warehouseId(), request.quantity());
    }

    @PostMapping("/reservations/{id}/release")
    @PreAuthorize("hasAuthority('P_INVENTORY_MANAGE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void releaseReservation(@PathVariable String id) {
        inventoryService.releaseReservation(id);
    }
}
