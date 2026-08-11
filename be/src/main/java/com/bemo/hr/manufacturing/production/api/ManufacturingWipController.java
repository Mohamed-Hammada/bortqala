package com.bemo.hr.manufacturing.production.api;

import com.bemo.hr.manufacturing.production.application.ManufacturingWipService;
import com.bemo.hr.manufacturing.production.domain.MaterialReservationHeader;
import com.bemo.hr.manufacturing.production.domain.MaterialReservationLine;
import com.bemo.hr.manufacturing.production.domain.WipPostingRecord;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/manufacturing/wip")
public class ManufacturingWipController {

    private final ManufacturingWipService wipService;

    public ManufacturingWipController(ManufacturingWipService wipService) {
        this.wipService = wipService;
    }

    public record CreateReservationPayload(String workOrderId) {}
    public record AddReservationLinePayload(String itemId, BigDecimal reservedQuantity) {}
    public record PostWipPayload(String workOrderId, String workCenterId, BigDecimal laborHours, BigDecimal machineHours, BigDecimal totalWipCost) {}

    @PostMapping("/reservations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRODUCTION_MANAGER')")
    public MaterialReservationHeader createReservation(@RequestBody CreateReservationPayload payload) {
        return wipService.createReservation(payload.workOrderId());
    }

    @PostMapping("/reservations/{id}/lines")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRODUCTION_MANAGER')")
    public MaterialReservationLine addReservationLine(@PathVariable String id, @RequestBody AddReservationLinePayload payload) {
        return wipService.addReservationLine(id, payload.itemId(), payload.reservedQuantity());
    }

    @PostMapping("/postings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRODUCTION_MANAGER', 'FINANCE_MANAGER')")
    public WipPostingRecord postWip(@RequestBody PostWipPayload payload) {
        return wipService.postWip(payload.workOrderId(), payload.workCenterId(), payload.laborHours(), payload.machineHours(), payload.totalWipCost());
    }

    @GetMapping("/postings/work-orders/{workOrderId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRODUCTION_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public List<WipPostingRecord> getWipPostings(@PathVariable String workOrderId) {
        return wipService.getWipPostings(workOrderId);
    }
}
