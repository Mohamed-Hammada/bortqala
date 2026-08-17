package com.bemo.hr.operations.api;

import com.bemo.hr.operations.application.WarehouseInventoryService;
import com.bemo.hr.operations.domain.StockReservation;
import com.bemo.hr.operations.domain.StockStatusBalance;
import com.bemo.hr.operations.domain.WarehouseBin;
import com.bemo.hr.organization.domain.Warehouse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/operations")
public class WarehouseInventoryController {

    private final WarehouseInventoryService inventoryService;

    public WarehouseInventoryController(WarehouseInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/warehouses")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public Warehouse createWarehouse(@RequestBody CreateWarehousePayload payload) {
        return inventoryService.createWarehouse(payload.branchId(), payload.code(), payload.name(), payload.location());
    }

    @PostMapping("/warehouses/{id}/bins")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public WarehouseBin createBin(@PathVariable String id, @RequestBody CreateBinPayload payload) {
        return inventoryService.createBin(id, payload.binCode(), payload.aisle(), payload.rack(), payload.shelf());
    }

    @GetMapping("/warehouses/{id}/bins")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER', 'VIEWER')")
    public List<WarehouseBin> bins(@PathVariable String id) {
        return inventoryService.bins(id);
    }

    @GetMapping("/warehouses/{id}/status-balances")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER', 'VIEWER')")
    public List<StockStatusBalance> balances(@PathVariable String id) {
        return inventoryService.balances(id);
    }

    @PostMapping("/warehouses/{id}/status-movements")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public void moveStatus(@PathVariable String id, @RequestBody MoveStatusPayload payload, Authentication authentication) {
        inventoryService.moveStatus(id, payload.binId(), payload.itemId(), payload.fromStatus(), payload.toStatus(),
                payload.quantity(), authentication.getName());
    }

    @PostMapping("/reservations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER', 'SALES_MANAGER')")
    public StockReservation reserveStock(@RequestBody ReserveStockPayload payload) {
        return inventoryService.reserveStock(payload.reservationNumber(), payload.sourceType(), payload.sourceId(), payload.itemId(), payload.warehouseId(), payload.quantity());
    }

    @PostMapping("/reservations/{id}/fulfill")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public StockReservation fulfillReservation(@PathVariable String id) {
        return inventoryService.fulfillReservation(id);
    }

    @PostMapping("/reservations/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public StockReservation cancelReservation(@PathVariable String id) {
        return inventoryService.cancelReservation(id);
    }

    @PostMapping("/reservations/{id}/expire")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public StockReservation expireReservation(@PathVariable String id) {
        return inventoryService.expireReservation(id);
    }

    @GetMapping("/warehouses/{id}/items/{itemId}/available")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER', 'SALES_MANAGER', 'VIEWER')")
    public BigDecimal getAvailableStock(@PathVariable String id, @PathVariable String itemId) {
        return inventoryService.getAvailableStock(id, itemId);
    }

    public record CreateWarehousePayload(String branchId, String code, String name, String location) {
    }

    public record CreateBinPayload(String binCode, String aisle, String rack, String shelf) {
    }

    public record ReserveStockPayload(String reservationNumber, String sourceType, String sourceId, String itemId,
                                      String warehouseId, BigDecimal quantity) {
    }

    public record MoveStatusPayload(String binId, String itemId, StockStatusBalance.Status fromStatus,
                                    StockStatusBalance.Status toStatus, BigDecimal quantity) {
    }
}
