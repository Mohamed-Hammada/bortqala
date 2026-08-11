package com.bemo.hr.manufacturing.production.api;

import com.bemo.hr.manufacturing.production.application.ManufacturingExecutionService;
import com.bemo.hr.manufacturing.production.domain.ProductionReceipt;
import com.bemo.hr.manufacturing.production.domain.RoutingHeader;
import com.bemo.hr.manufacturing.production.domain.WorkCenter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/manufacturing")
public class ManufacturingExecutionController {

    private final ManufacturingExecutionService manufacturingExecutionService;

    public ManufacturingExecutionController(ManufacturingExecutionService manufacturingExecutionService) {
        this.manufacturingExecutionService = manufacturingExecutionService;
    }

    public record CreateWorkCenterPayload(String code, String name, BigDecimal hourlyRate, BigDecimal capacityHoursPerDay) {}
    public record CreateRoutingPayload(String routingCode, String name, String itemId) {}
    public record RecordReceiptPayload(String receiptNumber, String finishedItemId, BigDecimal receivedQuantity, String receiptDate, String warehouseId) {}

    @PostMapping("/work-centers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRODUCTION_MANAGER')")
    public WorkCenter createWorkCenter(@RequestBody CreateWorkCenterPayload payload) {
        return manufacturingExecutionService.createWorkCenter(payload.code(), payload.name(), payload.hourlyRate(), payload.capacityHoursPerDay());
    }

    @PostMapping("/routings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRODUCTION_MANAGER')")
    public RoutingHeader createRouting(@RequestBody CreateRoutingPayload payload) {
        return manufacturingExecutionService.createRouting(payload.routingCode(), payload.name(), payload.itemId());
    }

    @PostMapping("/orders/{id}/receipts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRODUCTION_MANAGER')")
    public ProductionReceipt recordReceipt(@PathVariable String id, @RequestBody RecordReceiptPayload payload) {
        return manufacturingExecutionService.recordReceipt(payload.receiptNumber(), id, payload.finishedItemId(), payload.receivedQuantity(), LocalDate.parse(payload.receiptDate()), payload.warehouseId());
    }

    @GetMapping("/orders/{id}/receipts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRODUCTION_MANAGER', 'VIEWER')")
    public List<ProductionReceipt> getReceipts(@PathVariable String id) {
        return manufacturingExecutionService.getReceiptsForOrder(id);
    }
}
