package com.bemo.hr.compliance.eta.api;

import com.bemo.hr.compliance.eta.application.EtaComplianceService;
import com.bemo.hr.compliance.eta.domain.EtaDocumentType;
import com.bemo.hr.compliance.eta.domain.EtaSubmissionStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<EtaComplianceApi.ConfigResponse> getConfig() {
        return complianceService.getConfig()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/config")
    public ResponseEntity<EtaComplianceApi.ConfigResponse> saveConfig(@Valid @RequestBody EtaComplianceApi.SaveConfigRequest request) {
        return ResponseEntity.ok(complianceService.saveConfig(request));
    }

    @GetMapping("/summary")
    public ResponseEntity<EtaComplianceApi.SubmissionSummaryResponse> getSummary() {
        return ResponseEntity.ok(complianceService.getSummary());
    }

    @GetMapping("/submissions")
    public ResponseEntity<List<EtaComplianceApi.SubmissionResponse>> listSubmissions(
            @RequestParam(required = false) EtaSubmissionStatus status,
            @RequestParam(required = false) EtaDocumentType documentType) {
        return ResponseEntity.ok(complianceService.listSubmissions(status, documentType));
    }

    @PostMapping("/submissions/queue")
    public ResponseEntity<EtaComplianceApi.SubmissionResponse> queueInvoice(
            @Valid @RequestBody EtaComplianceApi.QueueInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(complianceService.queueInvoice(request));
    }

    @PostMapping("/submissions/{id}/submit")
    public ResponseEntity<EtaComplianceApi.SubmissionResponse> submitToEta(@PathVariable String id) {
        return ResponseEntity.ok(complianceService.submitToEta(id));
    }

    @PostMapping("/submissions/{id}/cancel")
    public ResponseEntity<EtaComplianceApi.SubmissionResponse> cancelDocument(
            @PathVariable String id,
            @Valid @RequestBody EtaComplianceApi.CancelDocumentRequest request) {
        return ResponseEntity.ok(complianceService.cancelDocument(id, request.reason()));
    }

    @GetMapping("/item-mappings")
    public ResponseEntity<List<EtaComplianceApi.ItemMappingResponse>> listItemMappings() {
        return ResponseEntity.ok(complianceService.listItemMappings());
    }

    @PostMapping("/item-mappings")
    public ResponseEntity<EtaComplianceApi.ItemMappingResponse> saveItemMapping(
            @Valid @RequestBody EtaComplianceApi.SaveItemMappingRequest request) {
        return ResponseEntity.ok(complianceService.saveItemMapping(request));
    }
}
