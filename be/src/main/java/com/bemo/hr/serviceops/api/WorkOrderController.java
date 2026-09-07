package com.bemo.hr.serviceops.api;

import com.bemo.hr.serviceops.application.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * No {@code @TenantId}-scoped entity here carries a branchId, so no branch-access check applies
 * to this module — confirmed by inspection of WorkOrder/WorkOrderLaborLine/WorkOrderPartsLine
 * during the 2026-09-06 security remediation.
 */
@RestController
@RequestMapping("/api/v1/service-ops/work-orders")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'INVENTORY_MANAGER', 'GENERAL_MANAGER')")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'INVENTORY_MANAGER')")
    public ServiceOpsApi.WorkOrderResponse createWorkOrder(@Valid @RequestBody ServiceOpsApi.WorkOrderCreateRequest request) {
        return workOrderService.createWorkOrder(request);
    }

    @GetMapping
    public List<ServiceOpsApi.WorkOrderResponse> listWorkOrders() {
        return workOrderService.listWorkOrders();
    }

    @GetMapping("/{id}")
    public ServiceOpsApi.WorkOrderResponse getWorkOrder(@PathVariable String id) {
        return workOrderService.getWorkOrder(id);
    }

    @PostMapping("/{id}/labor")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'INVENTORY_MANAGER')")
    public ServiceOpsApi.WorkOrderResponse addLaborLine(
            @PathVariable String id,
            @Valid @RequestBody ServiceOpsApi.AddLaborLineRequest request) {
        return workOrderService.addLaborLine(id, request);
    }

    @PostMapping("/{id}/parts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'INVENTORY_MANAGER')")
    public ServiceOpsApi.WorkOrderResponse addPartsLine(
            @PathVariable String id,
            @Valid @RequestBody ServiceOpsApi.AddPartsLineRequest request) {
        return workOrderService.addPartsLine(id, request);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'INVENTORY_MANAGER')")
    public ServiceOpsApi.WorkOrderResponse updateStatus(
            @PathVariable String id,
            @Valid @RequestBody ServiceOpsApi.UpdateWorkOrderStatusRequest request) {
        return workOrderService.updateStatus(id, request);
    }

    // Invoice-generating action: restricted further than plain writes (excludes INVENTORY_MANAGER).
    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER')")
    public ServiceOpsApi.WorkOrderResponse deliverAndCreateInvoice(@PathVariable String id) {
        return workOrderService.deliverAndCreateInvoice(id);
    }
}
