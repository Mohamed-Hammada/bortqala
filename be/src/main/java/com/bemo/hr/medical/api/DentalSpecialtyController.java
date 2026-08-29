package com.bemo.hr.medical.api;

import com.bemo.hr.medical.api.MedicalClinicApi.AddDentalPlanItemRequest;
import com.bemo.hr.medical.api.MedicalClinicApi.CreateDentalPlanRequest;
import com.bemo.hr.medical.api.MedicalClinicApi.DentalRecordDto;
import com.bemo.hr.medical.api.MedicalClinicApi.DentalTreatmentPlanDto;
import com.bemo.hr.medical.api.MedicalClinicApi.DentalTreatmentPlanItemDto;
import com.bemo.hr.medical.api.MedicalClinicApi.ExamAnswerDto;
import com.bemo.hr.medical.api.MedicalClinicApi.ExamTemplateDto;
import com.bemo.hr.medical.api.MedicalClinicApi.PatientOdontogramDto;
import com.bemo.hr.medical.api.MedicalClinicApi.RecordToothConditionRequest;
import com.bemo.hr.medical.api.MedicalClinicApi.SaveExamTemplateRequest;
import com.bemo.hr.medical.api.MedicalClinicApi.SubmitExamAnswerRequest;
import com.bemo.hr.medical.application.DentalSpecialtyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clinic")
@RequiredArgsConstructor
public class DentalSpecialtyController {

    private final DentalSpecialtyService dentalSpecialtyService;

    // --- Dental Odontogram & Records ---

    @PostMapping("/dental/patients/{patientId}/records")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('clinic.dental.manage') or hasRole('ADMIN')")
    public DentalRecordDto recordToothCondition(
            @PathVariable String patientId,
            @Valid @RequestBody RecordToothConditionRequest request
    ) {
        return dentalSpecialtyService.recordToothCondition(patientId, request);
    }

    @GetMapping("/dental/patients/{patientId}/odontogram")
    @PreAuthorize("@auth.hasPermission('clinic.dental.read') or hasRole('ADMIN')")
    public PatientOdontogramDto getPatientOdontogram(@PathVariable String patientId) {
        return dentalSpecialtyService.getPatientOdontogram(patientId);
    }

    // --- Treatment Plans ---

    @PostMapping("/dental/patients/{patientId}/plans")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('clinic.dental.manage') or hasRole('ADMIN')")
    public DentalTreatmentPlanDto createTreatmentPlan(
            @PathVariable String patientId,
            @Valid @RequestBody CreateDentalPlanRequest request
    ) {
        return dentalSpecialtyService.createTreatmentPlan(patientId, request);
    }

    @GetMapping("/dental/patients/{patientId}/plans")
    @PreAuthorize("@auth.hasPermission('clinic.dental.read') or hasRole('ADMIN')")
    public List<DentalTreatmentPlanDto> getTreatmentPlans(@PathVariable String patientId) {
        return dentalSpecialtyService.getTreatmentPlans(patientId);
    }

    @PostMapping("/dental/plans/{planId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('clinic.dental.manage') or hasRole('ADMIN')")
    public DentalTreatmentPlanItemDto addPlanItem(
            @PathVariable String planId,
            @Valid @RequestBody AddDentalPlanItemRequest request
    ) {
        return dentalSpecialtyService.addPlanItem(planId, request);
    }

    @PostMapping("/dental/plans/items/{itemId}/done")
    @PreAuthorize("@auth.hasPermission('clinic.dental.manage') or hasRole('ADMIN')")
    public DentalTreatmentPlanItemDto markPlanItemDone(
            @PathVariable String itemId,
            @RequestParam(required = false) String visitId
    ) {
        return dentalSpecialtyService.markPlanItemDone(itemId, visitId);
    }

    // --- Specialty Exam Templates ---

    @GetMapping("/exam-templates")
    @PreAuthorize("@auth.hasPermission('clinic.dental.read') or hasRole('ADMIN')")
    public List<ExamTemplateDto> getExamTemplates(@RequestParam(required = false) String specialty) {
        return dentalSpecialtyService.getExamTemplates(specialty);
    }

    @PostMapping("/exam-templates")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('clinic.dental.manage') or hasRole('ADMIN')")
    public ExamTemplateDto saveExamTemplate(@Valid @RequestBody SaveExamTemplateRequest request) {
        return dentalSpecialtyService.saveExamTemplate(request);
    }

    @PostMapping("/exam-templates/answers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('clinic.dental.manage') or hasRole('ADMIN')")
    public ExamAnswerDto submitExamAnswers(@Valid @RequestBody SubmitExamAnswerRequest request) {
        return dentalSpecialtyService.submitExamAnswers(request);
    }

    @GetMapping("/exam-templates/answers/visit/{visitId}")
    @PreAuthorize("@auth.hasPermission('clinic.dental.read') or hasRole('ADMIN')")
    public List<ExamAnswerDto> getExamAnswers(@PathVariable String visitId) {
        return dentalSpecialtyService.getExamAnswers(visitId);
    }
}
