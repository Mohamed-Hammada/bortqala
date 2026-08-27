package com.bemo.hr.trade.export.api;

import com.bemo.hr.trade.export.application.ExportShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trade/export-shipments")
@PreAuthorize("@auth.hasPermission('procurement.read')")
public class ExportShipmentController {

    private final ExportShipmentService service;

    public ExportShipmentController(ExportShipmentService service) {
        this.service = service;
    }

    // ─── Shipments ──────────────────────────────────────────────────

    @GetMapping
    public List<ExportShipmentApi.ShipmentResponse> list() {
        return service.listShipments();
    }

    @GetMapping("/{id}")
    public ExportShipmentApi.ShipmentResponse get(@PathVariable String id) {
        return service.getShipment(id);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public ExportShipmentApi.ShipmentResponse create(@Valid @RequestBody ExportShipmentApi.ShipmentPayload payload) {
        return service.createShipment(payload);
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public ExportShipmentApi.ShipmentResponse update(@PathVariable String id,
                                                     @Valid @RequestBody ExportShipmentApi.ShipmentPayload payload) {
        return service.updateShipment(id, payload);
    }

    @PostMapping("/{id}/transition")
    @Transactional
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public ExportShipmentApi.ShipmentResponse transition(@PathVariable String id,
                                                         @RequestParam String status) {
        return service.transitionShipment(id, status);
    }

    // ─── Treatment Logs / Compliance ─────────────────────────────────

    @GetMapping("/compliance/treatments")
    public List<ExportShipmentApi.TreatmentLogResponse> listTreatments(
            @RequestParam String lotReference) {
        return service.listTreatmentLogs(lotReference);
    }

    @PostMapping("/compliance/treatments")
    @Transactional
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public ExportShipmentApi.TreatmentLogResponse createTreatment(
            @Valid @RequestBody ExportShipmentApi.TreatmentLogPayload payload) {
        return service.createTreatmentLog(payload);
    }

    @GetMapping("/compliance/check")
    public ExportShipmentApi.ComplianceCheckResponse checkCompliance(
            @RequestParam List<String> lotReferences,
            @RequestParam long pickupDate) {
        LocalDate pickup = Instant.ofEpochMilli(pickupDate).atZone(ZoneOffset.UTC).toLocalDate();
        return service.checkCompliance(lotReferences, pickup);
    }

    // ─── Pesticide Register ──────────────────────────────────────────

    @GetMapping("/pesticides")
    public List<ExportShipmentApi.PesticideResponse> listPesticides() {
        return service.listPesticides();
    }

    @PostMapping("/pesticides")
    @Transactional
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public ExportShipmentApi.PesticideResponse createPesticide(
            @Valid @RequestBody ExportShipmentApi.PesticidePayload payload) {
        return service.createPesticide(payload);
    }

    @PutMapping("/pesticides/{id}")
    @Transactional
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public ExportShipmentApi.PesticideResponse updatePesticide(
            @PathVariable String id,
            @Valid @RequestBody ExportShipmentApi.PesticidePayload payload) {
        return service.updatePesticide(id, payload);
    }

    // ─── Proceeds & Aging ────────────────────────────────────────────

    @PostMapping("/{id}/proceeds")
    @Transactional
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public ExportShipmentApi.ProceedsResponse recordProceeds(
            @PathVariable String id,
            @Valid @RequestBody ExportShipmentApi.ProceedsPayload payload) {
        return service.recordProceeds(id, payload);
    }

    @GetMapping("/aging")
    public ExportShipmentApi.AgingResponse getAging() {
        return service.getAging();
    }
}
