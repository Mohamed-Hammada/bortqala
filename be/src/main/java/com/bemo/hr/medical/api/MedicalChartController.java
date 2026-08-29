package com.bemo.hr.medical.api;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.MedicalChartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clinic")
public class MedicalChartController {

    private final MedicalChartService chartService;

    public MedicalChartController(MedicalChartService chartService) {
        this.chartService = chartService;
    }

    @GetMapping("/patients/{patientId}/chart")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.patients.manage', 'clinic.queue.read', 'clinic.queue.manage')")
    public ResponseEntity<PatientChartResponse> getPatientChart(@PathVariable String patientId) {
        return ResponseEntity.ok(chartService.getPatientChart(patientId));
    }

    @PostMapping("/patients/{patientId}/allergies")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<PatientAllergyDto> addAllergy(@PathVariable String patientId,
                                                        @Valid @RequestBody AddAllergyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chartService.addAllergy(patientId, request));
    }

    @DeleteMapping("/allergies/{allergyId}")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<Void> deleteAllergy(@PathVariable String allergyId) {
        chartService.deleteAllergy(allergyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/patients/{patientId}/conditions")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<PatientConditionDto> addCondition(@PathVariable String patientId,
                                                            @Valid @RequestBody AddConditionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chartService.addCondition(patientId, request));
    }

    @PutMapping("/conditions/{conditionId}")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<PatientConditionDto> updateCondition(@PathVariable String conditionId,
                                                               @Valid @RequestBody UpdateConditionRequest request) {
        return ResponseEntity.ok(chartService.updateCondition(conditionId, request));
    }

    @DeleteMapping("/conditions/{conditionId}")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<Void> deleteCondition(@PathVariable String conditionId) {
        chartService.deleteCondition(conditionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/visits/{visitId}/vitals")
    @PreAuthorize("@auth.hasAnyPermission('clinic.queue.manage', 'clinic.patients.manage')")
    public ResponseEntity<VisitVitalsDto> recordVitals(@PathVariable String visitId,
                                                       @RequestBody RecordVitalsRequest request) {
        return ResponseEntity.ok(chartService.recordVitals(visitId, request));
    }

    @PostMapping("/patients/{patientId}/documents")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<PatientDocumentDto> saveDocument(@PathVariable String patientId,
                                                           @Valid @RequestBody UploadDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chartService.saveDocument(patientId, request));
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<Void> deleteDocument(@PathVariable String documentId) {
        chartService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/patients/{patientId}/consents")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<ConsentFormDto> signConsent(@PathVariable String patientId,
                                                      @Valid @RequestBody SignConsentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chartService.signConsent(patientId, request));
    }
}
