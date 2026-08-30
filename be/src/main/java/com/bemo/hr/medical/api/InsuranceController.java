package com.bemo.hr.medical.api;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.InsuranceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clinic/insurance")
public class InsuranceController {

    private final InsuranceService insuranceService;

    public InsuranceController(InsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    @GetMapping("/payers")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.queue.read', 'clinic.insurance.read')")
    public ResponseEntity<List<InsurancePayerDto>> getAllPayers(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(insuranceService.getAllPayers(activeOnly));
    }

    @PostMapping("/payers")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.insurance.manage')")
    public ResponseEntity<InsurancePayerDto> savePayer(@Valid @RequestBody SaveInsurancePayerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insuranceService.savePayer(request));
    }

    @GetMapping("/plans")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.insurance.read')")
    public ResponseEntity<List<InsurancePlanDto>> getPlansByPayer(@RequestParam(required = false) String payerId) {
        return ResponseEntity.ok(insuranceService.getPlansByPayer(payerId));
    }

    @PostMapping("/plans")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.insurance.manage')")
    public ResponseEntity<InsurancePlanDto> savePlan(@Valid @RequestBody SaveInsurancePlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insuranceService.savePlan(request));
    }

    @GetMapping("/policies/patient/{patientId}")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.queue.read', 'clinic.insurance.read')")
    public ResponseEntity<List<PatientInsurancePolicyDto>> getPatientPolicies(@PathVariable String patientId) {
        return ResponseEntity.ok(insuranceService.getPatientPolicies(patientId));
    }

    @PostMapping("/policies")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.insurance.manage')")
    public ResponseEntity<PatientInsurancePolicyDto> attachPolicy(@Valid @RequestBody AttachInsurancePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insuranceService.attachPolicy(request));
    }

    @PostMapping("/split-calculate")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.queue.read', 'clinic.insurance.read')")
    public ResponseEntity<InsuranceSplitCalculationResult> calculateSplit(@Valid @RequestBody CalculateInsuranceSplitRequest request) {
        return ResponseEntity.ok(insuranceService.calculateSplit(request));
    }

    @GetMapping("/pre-auth")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.insurance.read')")
    public ResponseEntity<List<InsurancePreAuthorizationDto>> getPreAuthorizations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String patientId) {
        return ResponseEntity.ok(insuranceService.getPreAuthorizations(status, patientId));
    }

    @PostMapping("/pre-auth")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.insurance.manage')")
    public ResponseEntity<InsurancePreAuthorizationDto> requestPreAuthorization(@Valid @RequestBody RequestPreAuthorizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insuranceService.requestPreAuthorization(request));
    }

    @PostMapping("/pre-auth/{id}/decide")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.insurance.manage')")
    public ResponseEntity<InsurancePreAuthorizationDto> decidePreAuthorization(
            @PathVariable String id,
            @Valid @RequestBody DecidePreAuthorizationRequest request) {
        return ResponseEntity.ok(insuranceService.decidePreAuthorization(id, request));
    }

    @GetMapping("/claims/batches")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.insurance.read')")
    public ResponseEntity<List<InsuranceClaimBatchDto>> getAllClaimBatches(@RequestParam(required = false) String payerId) {
        return ResponseEntity.ok(insuranceService.getAllClaimBatches(payerId));
    }

    @PostMapping("/claims/batches")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.insurance.manage')")
    public ResponseEntity<InsuranceClaimBatchDto> createClaimBatch(@Valid @RequestBody CreateClaimBatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insuranceService.createClaimBatch(request));
    }

    @PostMapping("/claims/batches/{id}/submit")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.insurance.manage')")
    public ResponseEntity<InsuranceClaimBatchDto> submitClaimBatch(@PathVariable String id) {
        return ResponseEntity.ok(insuranceService.submitClaimBatch(id));
    }

    @PostMapping("/claims/batches/{id}/settle")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.insurance.manage')")
    public ResponseEntity<InsuranceClaimBatchDto> settleClaimBatch(
            @PathVariable String id,
            @Valid @RequestBody SettleClaimBatchRequest request) {
        return ResponseEntity.ok(insuranceService.settleClaimBatch(id, request));
    }

    @PostMapping("/claims/lines/resubmit")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.insurance.manage')")
    public ResponseEntity<InsuranceClaimLineDto> resubmitClaimLine(@Valid @RequestBody ResubmitClaimLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insuranceService.resubmitClaimLine(request));
    }
}
