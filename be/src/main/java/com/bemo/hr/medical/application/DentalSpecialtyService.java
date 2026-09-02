package com.bemo.hr.medical.application;

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
import com.bemo.hr.medical.api.MedicalClinicApi.ToothStatusSummaryDto;
import com.bemo.hr.medical.domain.DentalRecord;
import com.bemo.hr.medical.domain.DentalTreatmentPlan;
import com.bemo.hr.medical.domain.DentalTreatmentPlanItem;
import com.bemo.hr.medical.domain.ExamAnswer;
import com.bemo.hr.medical.domain.ExamTemplate;
import com.bemo.hr.medical.domain.Patient;
import com.bemo.hr.medical.infrastructure.DentalRecordRepository;
import com.bemo.hr.medical.infrastructure.DentalTreatmentPlanItemRepository;
import com.bemo.hr.medical.infrastructure.DentalTreatmentPlanRepository;
import com.bemo.hr.medical.infrastructure.ExamAnswerRepository;
import com.bemo.hr.medical.infrastructure.ExamTemplateRepository;
import com.bemo.hr.medical.infrastructure.PatientRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DentalSpecialtyService {

    private static final Set<Integer> VALID_ADULT_FDI_TEETH = Set.of(
            11, 12, 13, 14, 15, 16, 17, 18,
            21, 22, 23, 24, 25, 26, 27, 28,
            31, 32, 33, 34, 35, 36, 37, 38,
            41, 42, 43, 44, 45, 46, 47, 48
    );

    private final DentalRecordRepository dentalRecordRepository;
    private final DentalTreatmentPlanRepository dentalTreatmentPlanRepository;
    private final DentalTreatmentPlanItemRepository dentalTreatmentPlanItemRepository;
    private final ExamTemplateRepository examTemplateRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final PatientRepository patientRepository;

    private String getAppId() {
        return TenantContext.require();
    }

    private void validateFdiToothNumber(int toothNumber) {
        if (!VALID_ADULT_FDI_TEETH.contains(toothNumber)) {
            throw new BusinessRuleException(
                    "Invalid FDI tooth number " + toothNumber + ". Must be between 11..18, 21..28, 31..38, 41..48.",
                    "DENTAL_TOOTH_INVALID",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    // --- Dental Odontogram & Records ---

    @Transactional
    public DentalRecordDto recordToothCondition(String patientId, RecordToothConditionRequest request) {
        String appId = getAppId();
        Patient patient = patientRepository.findByAppIdAndId(appId, patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + patientId, "PATIENT_NOT_FOUND"));

        validateFdiToothNumber(request.toothNumber());

        DentalRecord.Condition condition;
        try {
            condition = DentalRecord.Condition.valueOf(request.condition().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            condition = DentalRecord.Condition.CARIES;
        }

        DentalRecord.Surface surface = null;
        if (request.surface() != null && !request.surface().isBlank()) {
            try {
                surface = DentalRecord.Surface.valueOf(request.surface().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                surface = null;
            }
        }

        DentalRecord record = new DentalRecord(
                patient.getId(),
                request.visitId(),
                request.toothNumber(),
                condition,
                surface,
                request.notes(),
                request.notedOn()
        );

        DentalRecord saved = dentalRecordRepository.save(record);
        return mapToDentalRecordDto(saved);
    }

    @Transactional(readOnly = true)
    public PatientOdontogramDto getPatientOdontogram(String patientId) {
        String appId = getAppId();
        Patient patient = patientRepository.findByAppIdAndId(appId, patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + patientId, "PATIENT_NOT_FOUND"));

        List<DentalRecord> allRecords = dentalRecordRepository.findAllByAppIdAndPatientIdOrderByNotedOnDesc(appId, patientId);

        // Aggregate latest state per tooth
        Map<Integer, DentalRecord> latestPerTooth = allRecords.stream()
                .collect(Collectors.toMap(
                        DentalRecord::getToothNumber,
                        r -> r,
                        (existing, newer) -> existing // allRecords is sorted by notedOn desc, so first is newest
                ));

        List<ToothStatusSummaryDto> teeth = new ArrayList<>();
        for (Integer toothNum : VALID_ADULT_FDI_TEETH.stream().sorted().toList()) {
            DentalRecord latest = latestPerTooth.get(toothNum);
            if (latest != null) {
                teeth.add(new ToothStatusSummaryDto(
                        toothNum,
                        latest.getCondition().name(),
                        latest.getSurface() != null ? latest.getSurface().name() : null,
                        latest.getNotes(),
                        latest.getNotedOn()
                ));
            } else {
                teeth.add(new ToothStatusSummaryDto(
                        toothNum,
                        DentalRecord.Condition.HEALTHY.name(),
                        null,
                        null,
                        0L
                ));
            }
        }

        List<DentalRecordDto> history = allRecords.stream()
                .map(this::mapToDentalRecordDto)
                .toList();

        return new PatientOdontogramDto(
                patient.getId(),
                patient.getFullName(),
                patient.getMrn(),
                teeth,
                history
        );
    }

    // --- Treatment Plans ---

    @Transactional
    public DentalTreatmentPlanDto createTreatmentPlan(String patientId, CreateDentalPlanRequest request) {
        String appId = getAppId();
        Patient patient = patientRepository.findByAppIdAndId(appId, patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + patientId, "PATIENT_NOT_FOUND"));

        DentalTreatmentPlan plan = new DentalTreatmentPlan(patient.getId(), request.title());
        DentalTreatmentPlan saved = dentalTreatmentPlanRepository.save(plan);
        return mapToTreatmentPlanDto(saved, List.of());
    }

    @Transactional
    public DentalTreatmentPlanItemDto addPlanItem(String planId, AddDentalPlanItemRequest request) {
        String appId = getAppId();
        DentalTreatmentPlan plan = dentalTreatmentPlanRepository.findByAppIdAndId(appId, planId)
                .orElseThrow(() -> new NotFoundException("Treatment plan not found: " + planId, "DENTAL_PLAN_NOT_FOUND"));

        validateFdiToothNumber(request.toothNumber());

        DentalTreatmentPlanItem item = new DentalTreatmentPlanItem(
                plan.getId(),
                request.toothNumber(),
                request.procedureText(),
                request.estimatedCost()
        );

        DentalTreatmentPlanItem saved = dentalTreatmentPlanItemRepository.save(item);
        return mapToPlanItemDto(saved);
    }

    @Transactional
    public DentalTreatmentPlanItemDto markPlanItemDone(String itemId, String visitId) {
        String appId = getAppId();
        DentalTreatmentPlanItem item = dentalTreatmentPlanItemRepository.findByAppIdAndId(appId, itemId)
                .orElseThrow(() -> new NotFoundException("Plan item not found: " + itemId, "DENTAL_ITEM_NOT_FOUND"));

        if (item.getStatus() == DentalTreatmentPlanItem.Status.DONE) {
            return mapToPlanItemDto(item); // Idempotent
        }

        item.setStatus(DentalTreatmentPlanItem.Status.DONE);
        item.setCompletedAt(System.currentTimeMillis());
        item.setVisitId(visitId);

        DentalTreatmentPlanItem updated = dentalTreatmentPlanItemRepository.save(item);
        return mapToPlanItemDto(updated);
    }

    @Transactional(readOnly = true)
    public List<DentalTreatmentPlanDto> getTreatmentPlans(String patientId) {
        String appId = getAppId();
        List<DentalTreatmentPlan> plans = dentalTreatmentPlanRepository.findAllByAppIdAndPatientIdOrderByCreatedAtDesc(appId, patientId);

        return plans.stream().map(p -> {
            List<DentalTreatmentPlanItem> items = dentalTreatmentPlanItemRepository.findAllByAppIdAndPlanIdOrderByToothNumberAsc(appId, p.getId());
            return mapToTreatmentPlanDto(p, items);
        }).toList();
    }

    // --- Specialty Exam Templates ---

    @Transactional(readOnly = true)
    public List<ExamTemplateDto> getExamTemplates(String specialty) {
        String appId = getAppId();
        List<ExamTemplate> templates = (specialty != null && !specialty.isBlank())
                ? examTemplateRepository.findAllByAppIdAndSpecialtyAndActiveOrderByNameAsc(appId, specialty, true)
                : examTemplateRepository.findAllByAppIdAndActiveOrderBySpecialtyAscNameAsc(appId, true);

        return templates.stream().map(this::mapToExamTemplateDto).toList();
    }

    @Transactional
    public ExamTemplateDto saveExamTemplate(SaveExamTemplateRequest request) {
        ExamTemplate template = new ExamTemplate(request.specialty(), request.name(), request.schemaJson());
        ExamTemplate saved = examTemplateRepository.save(template);
        return mapToExamTemplateDto(saved);
    }

    @Transactional
    public ExamAnswerDto submitExamAnswers(SubmitExamAnswerRequest request) {
        String appId = getAppId();
        ExamTemplate template = examTemplateRepository.findByAppIdAndId(appId, request.templateId())
                .orElseThrow(() -> new NotFoundException("Template not found: " + request.templateId(), "EXAM_TEMPLATE_NOT_FOUND"));

        ExamAnswer answer = new ExamAnswer(
                request.visitId(),
                template.getId(),
                request.answersJson(),
                request.recordedBy()
        );

        ExamAnswer saved = examAnswerRepository.save(answer);
        return mapToExamAnswerDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ExamAnswerDto> getExamAnswers(String visitId) {
        String appId = getAppId();
        List<ExamAnswer> answers = examAnswerRepository.findAllByAppIdAndVisitIdOrderByRecordedAtDesc(appId, visitId);
        return answers.stream().map(this::mapToExamAnswerDto).toList();
    }

    // --- Mappers ---

    private DentalRecordDto mapToDentalRecordDto(DentalRecord r) {
        return new DentalRecordDto(
                r.getId(),
                r.getPatientId(),
                r.getVisitId(),
                r.getToothNumber(),
                r.getCondition().name(),
                r.getSurface() != null ? r.getSurface().name() : null,
                r.getNotes(),
                r.getNotedOn()
        );
    }

    private DentalTreatmentPlanDto mapToTreatmentPlanDto(DentalTreatmentPlan p, List<DentalTreatmentPlanItem> items) {
        List<DentalTreatmentPlanItemDto> itemDtos = items.stream().map(this::mapToPlanItemDto).toList();
        return new DentalTreatmentPlanDto(
                p.getId(),
                p.getPatientId(),
                p.getTitle(),
                p.getStatus().name(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                itemDtos
        );
    }

    private DentalTreatmentPlanItemDto mapToPlanItemDto(DentalTreatmentPlanItem i) {
        return new DentalTreatmentPlanItemDto(
                i.getId(),
                i.getPlanId(),
                i.getToothNumber(),
                i.getProcedureText(),
                i.getEstimatedCost(),
                i.getStatus().name(),
                i.getCompletedAt(),
                i.getVisitId()
        );
    }

    private ExamTemplateDto mapToExamTemplateDto(ExamTemplate t) {
        return new ExamTemplateDto(
                t.getId(),
                t.getSpecialty(),
                t.getName(),
                t.getSchemaJson(),
                t.getActive(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    private ExamAnswerDto mapToExamAnswerDto(ExamAnswer a) {
        return new ExamAnswerDto(
                a.getId(),
                a.getVisitId(),
                a.getTemplateId(),
                a.getAnswersJson(),
                a.getRecordedAt(),
                a.getRecordedBy()
        );
    }
}
