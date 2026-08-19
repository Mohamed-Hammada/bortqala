package com.bemo.hr.manufacturing.production.api;

import com.bemo.hr.manufacturing.production.application.ManufacturingService;
import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manufacturing")
@PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER + " or " + Roles.MANUFACTURING_MANAGER + " or " + Roles.QUALITY_MANAGER)
public class ManufacturingController {

    private final ManufacturingService manufacturingService;

    public ManufacturingController(ManufacturingService manufacturingService) {
        this.manufacturingService = manufacturingService;
    }

    // ─── BOMs ───────────────────────────────────────────────────────
    @GetMapping("/boms")
    public List<ManufacturingApi.BomResponse> listBoms() {
        return manufacturingService.listBoms();
    }

    @PostMapping("/boms")
    @Transactional
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.MANUFACTURING_MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    public ManufacturingApi.BomResponse createBom(@Valid @RequestBody ManufacturingApi.BomPayload payload) {
        return manufacturingService.createBom(payload);
    }

    @PutMapping("/boms/{id}")
    @Transactional
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.MANUFACTURING_MANAGER)
    public ManufacturingApi.BomResponse updateBom(@PathVariable String id, @Valid @RequestBody ManufacturingApi.BomPayload payload) {
        return manufacturingService.updateBom(id, payload);
    }

    // ─── Production Work Orders ─────────────────────────────────────
    @GetMapping("/orders")
    public List<ManufacturingApi.ProductionOrderResponse> listProductionOrders() {
        return manufacturingService.listProductionOrders();
    }

    @PostMapping("/orders")
    @Transactional
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.MANUFACTURING_MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    public ManufacturingApi.ProductionOrderResponse createProductionOrder(@Valid @RequestBody ManufacturingApi.ProductionOrderPayload payload) {
        return manufacturingService.createProductionOrder(payload);
    }

    @GetMapping("/orders/{id}/readiness")
    public ManufacturingApi.MaterialReadinessResponse checkReadiness(@PathVariable String id) {
        return manufacturingService.checkMaterialReadiness(id);
    }

    @PostMapping("/orders/{id}/start")
    @Transactional
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.MANUFACTURING_MANAGER)
    public ManufacturingApi.ProductionOrderResponse startProductionOrder(@PathVariable String id) {
        return manufacturingService.startProductionOrder(id);
    }

    @PostMapping("/orders/{id}/complete")
    @Transactional
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.MANUFACTURING_MANAGER)
    public ManufacturingApi.ProductionOrderResponse completeProductionOrder(
            @PathVariable String id, @Valid @RequestBody ManufacturingApi.CompleteProductionOrderPayload payload) {
        return manufacturingService.completeProductionOrder(id, payload);
    }

    @PostMapping("/orders/{id}/cancel")
    @Transactional
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.MANUFACTURING_MANAGER)
    public ManufacturingApi.ProductionOrderResponse cancelProductionOrder(@PathVariable String id) {
        return manufacturingService.cancelProductionOrder(id);
    }

    // ─── Quality Control Inspections ────────────────────────────────
    @GetMapping("/quality")
    public List<ManufacturingApi.QualityInspectionResponse> listInspections() {
        return manufacturingService.listInspections();
    }

    @PostMapping("/quality")
    @Transactional
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.MANUFACTURING_MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    public ManufacturingApi.QualityInspectionResponse createInspection(@Valid @RequestBody ManufacturingApi.QualityInspectionPayload payload) {
        return manufacturingService.createInspection(payload);
    }
}
