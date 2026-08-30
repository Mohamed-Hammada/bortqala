package com.bemo.hr.operations.api;

import com.bemo.hr.operations.application.ItemLotSerialService;
import com.bemo.hr.operations.domain.ItemLotSerial;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/operations/lots-serials")
public class ItemLotSerialController {

    private final ItemLotSerialService service;

    public ItemLotSerialController(ItemLotSerialService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public ItemLotSerial createLotSerial(@RequestBody CreateLotSerialPayload payload) {
        LocalDate exp = payload.expirationDate() != null ? LocalDate.parse(payload.expirationDate()) : null;
        LocalDate mfg = payload.manufactureDate() != null ? LocalDate.parse(payload.manufactureDate()) : null;
        return service.createLotSerial(payload.itemId(), payload.lotNumber(), payload.serialNumber(), exp, mfg);
    }

    @PostMapping("/{id}/quarantine")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public ItemLotSerial quarantine(@PathVariable String id) {
        return service.quarantine(id);
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public ItemLotSerial block(@PathVariable String id) {
        return service.block(id);
    }

    @GetMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER', 'VIEWER')")
    public List<ItemLotSerial> getAvailableLotsByItem(@PathVariable String itemId) {
        return service.getAvailableLotsByItem(itemId);
    }

    @GetMapping("/{id}/trace")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER', 'SALES_MANAGER', 'VIEWER')")
    public ItemLotSerial trace(@PathVariable String id) {
        return service.trace(id);
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public ItemLotSerial issue(@PathVariable String id, @RequestBody LotSerialMovementPayload payload) {
        return service.issue(id, payload.quantity(), payload.documentReference());
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public ItemLotSerial receiveReturn(@PathVariable String id, @RequestBody LotSerialMovementPayload payload) {
        return service.receiveReturn(id, payload.quantity(), payload.documentReference());
    }

    /**
     * FEFO auto-pick: issues from the earliest-expiring lots for an item+warehouse.
     * Blocks expired lots automatically. Returns the list of lots that were issued.
     */
    @PostMapping("/pick/fefo")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public List<ItemLotSerial> pickFefo(@RequestBody PickRequest request) {
        return service.pickFefo(request.itemId(), request.warehouseId(), request.quantity(), request.documentReference());
    }

    /**
     * FIFO auto-pick: issues from the oldest lots for an item+warehouse.
     */
    @PostMapping("/pick/fifo")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER')")
    public List<ItemLotSerial> pickFifo(@RequestBody PickRequest request) {
        return service.pickFifo(request.itemId(), request.warehouseId(), request.quantity(), request.documentReference());
    }

    /**
     * Expiry warnings: lots expiring within N days.
     */
    @GetMapping("/expiring/{itemId}/{warehouseId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER', 'VIEWER')")
    public List<ItemLotSerial> getExpiringLots(
            @PathVariable String itemId, @PathVariable String warehouseId,
            @RequestParam(defaultValue = "30") int withinDays) {
        return service.getLotsExpiringWithinDays(itemId, warehouseId, withinDays);
    }

    public record PickRequest(String itemId, String warehouseId, BigDecimal quantity, String documentReference) {}

    public record CreateLotSerialPayload(String itemId, String lotNumber, String serialNumber, String expirationDate,
                                         String manufactureDate) {
    }

    public record LotSerialMovementPayload(BigDecimal quantity, String documentReference) {
    }
}
