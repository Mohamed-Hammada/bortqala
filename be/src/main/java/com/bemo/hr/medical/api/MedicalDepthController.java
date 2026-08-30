package com.bemo.hr.medical.api;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.MedicalDepthService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clinic/depth")
public class MedicalDepthController {

    private final MedicalDepthService depthService;

    public MedicalDepthController(MedicalDepthService depthService) {
        this.depthService = depthService;
    }

    @PostMapping("/patients/{patientId}/family")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('clinic.patients.manage') or hasRole('ADMIN')")
    public PatientFamilyLinkDto linkFamilyMember(
            @PathVariable String patientId,
            @Valid @RequestBody LinkFamilyMemberRequest req) {
        return depthService.linkFamilyMember(patientId, req);
    }

    @GetMapping("/patients/{patientId}/family")
    @PreAuthorize("@auth.hasPermission('clinic.patients.read') or hasRole('ADMIN')")
    public List<PatientFamilyLinkDto> getFamilyLinks(@PathVariable String patientId) {
        return depthService.getFamilyLinks(patientId);
    }

    @PostMapping("/calculator/pediatric-dose")
    @PreAuthorize("@auth.hasPermission('clinic.patients.read') or hasRole('ADMIN')")
    public PediatricDoseCalculationResponse calculatePediatricDose(
            @Valid @RequestBody PediatricDoseCalculationRequest req) {
        return depthService.calculatePediatricDose(req);
    }

    @PostMapping("/telemedicine/schedule")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('clinic.appointments.manage') or hasRole('ADMIN')")
    public TelemedicineSessionDto scheduleTelemedicineSession(
            @Valid @RequestBody ScheduleTelemedicineSessionRequest req) {
        return depthService.scheduleTelemedicineSession(req);
    }

    @GetMapping("/telemedicine/patient/{patientId}")
    @PreAuthorize("@auth.hasPermission('clinic.appointments.read') or hasRole('ADMIN')")
    public List<TelemedicineSessionDto> getTelemedicineSessionsByPatient(@PathVariable String patientId) {
        return depthService.getTelemedicineSessionsByPatient(patientId);
    }

    @PostMapping("/licenses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('clinic.patients.manage') or hasRole('ADMIN')")
    public MedicalLicenseRecordDto registerLicense(
            @Valid @RequestBody RegisterMedicalLicenseRequest req) {
        return depthService.registerLicense(req);
    }

    @GetMapping("/licenses")
    @PreAuthorize("@auth.hasPermission('clinic.patients.read') or hasRole('ADMIN')")
    public List<MedicalLicenseRecordDto> getAllLicenses() {
        return depthService.getAllLicenses();
    }
}
