package com.bemo.hr.manufacturing.production.api;

import com.bemo.hr.manufacturing.production.application.MaterialIssueService;
import com.bemo.hr.manufacturing.production.domain.MaterialIssueHeader;
import com.bemo.hr.manufacturing.production.domain.MaterialIssueLine;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/manufacturing/material-issues")
public class MaterialIssueController {

    private final MaterialIssueService issueService;

    public MaterialIssueController(MaterialIssueService issueService) {
        this.issueService = issueService;
    }

    public record CreateIssuePayload(String issueNumber, String productionOrderId, String issueDate) {}
    public record AddIssueLinePayload(String itemId, BigDecimal quantity, String warehouseId) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANUFACTURING_MANAGER', 'INVENTORY_MANAGER')")
    public MaterialIssueHeader createIssue(@RequestBody CreateIssuePayload payload) {
        return issueService.createIssue(payload.issueNumber(), payload.productionOrderId(), LocalDate.parse(payload.issueDate()));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANUFACTURING_MANAGER', 'INVENTORY_MANAGER')")
    public MaterialIssueLine addIssueLine(@PathVariable String id, @RequestBody AddIssueLinePayload payload) {
        return issueService.addIssueLine(id, payload.itemId(), payload.quantity(), payload.warehouseId());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANUFACTURING_MANAGER', 'INVENTORY_MANAGER')")
    public MaterialIssueHeader cancelIssue(@PathVariable String id) {
        return issueService.cancelIssue(id);
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANUFACTURING_MANAGER', 'INVENTORY_MANAGER', 'VIEWER')")
    public List<MaterialIssueHeader> getIssuesByOrder(@PathVariable String orderId) {
        return issueService.getIssuesByProductionOrder(orderId);
    }
}
