package com.bemo.hr.serviceops.api;

import com.bemo.hr.serviceops.application.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-ops/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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
    public ServiceOpsApi.WorkOrderResponse addLaborLine(
            @PathVariable String id,
            @Valid @RequestBody ServiceOpsApi.AddLaborLineRequest request) {
        return workOrderService.addLaborLine(id, request);
    }

    @PostMapping("/{id}/parts")
    public ServiceOpsApi.WorkOrderResponse addPartsLine(
            @PathVariable String id,
            @Valid @RequestBody ServiceOpsApi.AddPartsLineRequest request) {
        return workOrderService.addPartsLine(id, request);
    }

    @PostMapping("/{id}/status")
    public ServiceOpsApi.WorkOrderResponse updateStatus(
            @PathVariable String id,
            @Valid @RequestBody ServiceOpsApi.UpdateWorkOrderStatusRequest request) {
        return workOrderService.updateStatus(id, request);
    }

    @PostMapping("/{id}/deliver")
    public ServiceOpsApi.WorkOrderResponse deliverAndCreateInvoice(@PathVariable String id) {
        return workOrderService.deliverAndCreateInvoice(id);
    }
}
