package com.bemo.hr.trade.export.api;

import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.shared.security.AuthService;
import com.bemo.hr.trade.export.application.ExportShipmentDocService;
import com.bemo.hr.trade.export.application.ExportShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trade/export-shipments")
@PreAuthorize("@auth.hasPermission('procurement.read')")
public class ExportShipmentController {

    private final ExportShipmentService service;
    private final ExportShipmentDocService docService;
    private final AuthService authService;
    private final TranslationService translationService;

    public ExportShipmentController(ExportShipmentService service,
                                    ExportShipmentDocService docService,
                                    AuthService authService,
                                    TranslationService translationService) {
        this.service = service;
        this.docService = docService;
        this.authService = authService;
        this.translationService = translationService;
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

    // ─── Printable Documents ─────────────────────────────────────────

    @GetMapping("/{id}/docs/{type}.xlsx")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> document(@PathVariable String id,
                                           @PathVariable String type,
                                           Authentication authentication) {
        var preference = authService.currentPreferences(authentication.getName());
        ExportShipmentDocService.DocType docType = ExportShipmentDocService.DocType.fromRoute(type);
        byte[] body = docService.render(id, docType, preference.locale());

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String fileKey = "export.file." + docType.routeSegment();
        String base = preference.locale().startsWith("ar")
                ? translationService.translateOrDefault(fileKey, preference.locale(), docType.routeSegment())
                : docType.routeSegment();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(base + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".xlsx",
                        StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
