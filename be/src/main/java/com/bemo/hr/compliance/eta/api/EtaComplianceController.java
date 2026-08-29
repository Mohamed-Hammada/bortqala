package com.bemo.hr.compliance.eta.api;

import com.bemo.hr.compliance.eta.application.EtaComplianceService;
import com.bemo.hr.compliance.eta.domain.EtaDocumentType;
import com.bemo.hr.compliance.eta.domain.EtaSubmissionStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/compliance/eta")
public class EtaComplianceController {

    private final EtaComplianceService complianceService;

    public EtaComplianceController(EtaComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @GetMapping("/config")
    @PreAuthorize("@auth.hasPermission('etaTax.read')")
    public ResponseEntity<EtaComplianceApi.ConfigResponse> getConfig() {
        return complianceService.getConfig()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/config")
    @PreAuthorize("@auth.hasPermission('etaTax.manage')")
    public ResponseEntity<EtaComplianceApi.ConfigResponse> saveConfig(@Valid @RequestBody EtaComplianceApi.SaveConfigRequest request) {
        return ResponseEntity.ok(complianceService.saveConfig(request));
    }

    @GetMapping("/summary")
    @PreAuthorize("@auth.hasPermission('etaTax.read')")
    public ResponseEntity<EtaComplianceApi.SubmissionSummaryResponse> getSummary() {
        return ResponseEntity.ok(complianceService.getSummary());
    }

    @GetMapping("/submissions")
    @PreAuthorize("@auth.hasPermission('etaTax.read')")
    public ResponseEntity<List<EtaComplianceApi.SubmissionResponse>> listSubmissions(
            @RequestParam(required = false) EtaSubmissionStatus status,
            @RequestParam(required = false) EtaDocumentType documentType) {
        return ResponseEntity.ok(complianceService.listSubmissions(status, documentType));
    }

    @PostMapping("/submissions/queue")
    @PreAuthorize("@auth.hasPermission('etaTax.manage')")
    public ResponseEntity<EtaComplianceApi.SubmissionResponse> queueInvoice(
            @Valid @RequestBody EtaComplianceApi.QueueInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(complianceService.queueInvoice(request));
    }

    @PostMapping("/submissions/{id}/submit")
    @PreAuthorize("@auth.hasPermission('etaTax.manage')")
    public ResponseEntity<EtaComplianceApi.SubmissionResponse> submitToEta(@PathVariable String id) {
        return ResponseEntity.ok(complianceService.submitToEta(id));
    }

    @PostMapping("/submissions/{id}/cancel")
    @PreAuthorize("@auth.hasPermission('etaTax.manage')")
    public ResponseEntity<EtaComplianceApi.SubmissionResponse> cancelDocument(
            @PathVariable String id,
            @Valid @RequestBody EtaComplianceApi.CancelDocumentRequest request) {
        return ResponseEntity.ok(complianceService.cancelDocument(id, request.reason()));
    }

    @PostMapping("/submissions/adjustment-notes")
    @PreAuthorize("@auth.hasPermission('etaTax.manage')")
    public ResponseEntity<EtaComplianceApi.SubmissionResponse> createAdjustmentNote(
            @Valid @RequestBody EtaComplianceApi.CreateAdjustmentNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(complianceService.createAdjustmentNote(request));
    }

    @PostMapping("/submissions/{id}/retry")
    @PreAuthorize("@auth.hasPermission('etaTax.manage')")
    public ResponseEntity<EtaComplianceApi.SubmissionResponse> retrySubmission(@PathVariable String id) {
        return ResponseEntity.ok(complianceService.retrySubmission(id));
    }

    @PostMapping("/submissions/reconcile")
    @PreAuthorize("@auth.hasPermission('etaTax.manage')")
    public ResponseEntity<EtaComplianceApi.ReconciliationResponse> reconcileSubmissions() {
        return ResponseEntity.ok(complianceService.reconcileSubmissions());
    }

    @GetMapping("/item-mappings")
    @PreAuthorize("@auth.hasPermission('etaTax.read')")
    public ResponseEntity<List<EtaComplianceApi.ItemMappingResponse>> listItemMappings() {
        return ResponseEntity.ok(complianceService.listItemMappings());
    }

    @PostMapping("/item-mappings")
    @PreAuthorize("@auth.hasPermission('etaTax.manage')")
    public ResponseEntity<EtaComplianceApi.ItemMappingResponse> saveItemMapping(
            @Valid @RequestBody EtaComplianceApi.SaveItemMappingRequest request) {
        return ResponseEntity.ok(complianceService.saveItemMapping(request));
    }
}
