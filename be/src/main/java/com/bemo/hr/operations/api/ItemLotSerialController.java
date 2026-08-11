package com.bemo.hr.operations.api;

import com.bemo.hr.operations.application.ItemLotSerialService;
import com.bemo.hr.operations.domain.ItemLotSerial;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/operations/lots-serials")
public class ItemLotSerialController {

    private final ItemLotSerialService service;

    public ItemLotSerialController(ItemLotSerialService service) {
        this.service = service;
    }

    public record CreateLotSerialPayload(String itemId, String lotNumber, String serialNumber, String expirationDate, String manufactureDate) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER')")
    public ItemLotSerial createLotSerial(@RequestBody CreateLotSerialPayload payload) {
        LocalDate exp = payload.expirationDate() != null ? LocalDate.parse(payload.expirationDate()) : null;
        LocalDate mfg = payload.manufactureDate() != null ? LocalDate.parse(payload.manufactureDate()) : null;
        return service.createLotSerial(payload.itemId(), payload.lotNumber(), payload.serialNumber(), exp, mfg);
    }

    @PostMapping("/{id}/quarantine")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER')")
    public ItemLotSerial quarantine(@PathVariable String id) {
        return service.quarantine(id);
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER')")
    public ItemLotSerial block(@PathVariable String id) {
        return service.block(id);
    }

    @GetMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATIONS_MANAGER', 'VIEWER')")
    public List<ItemLotSerial> getAvailableLotsByItem(@PathVariable String itemId) {
        return service.getAvailableLotsByItem(itemId);
    }
}
