package com.bemo.hr.medical.application;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.domain.*;
import com.bemo.hr.medical.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class MedicalChartService {

    private final PatientRepository patientRepository;
    private final PatientAllergyRepository allergyRepository;
    private final PatientConditionRepository conditionRepository;
    private final VisitVitalsRepository vitalsRepository;
    private final PatientDocumentRepository documentRepository;
    private final ConsentFormRepository consentRepository;
    private final ClinicVisitRepository visitRepository;
    private final ClinicQueueService queueService;
    private final PatientService patientService;

    public MedicalChartService(PatientRepository patientRepository,
                               PatientAllergyRepository allergyRepository,
                               PatientConditionRepository conditionRepository,
                               VisitVitalsRepository vitalsRepository,
                               PatientDocumentRepository documentRepository,
                               ConsentFormRepository consentRepository,
                               ClinicVisitRepository visitRepository,
                               ClinicQueueService queueService,
                               PatientService patientService) {
        this.patientRepository = patientRepository;
        this.allergyRepository = allergyRepository;
        this.conditionRepository = conditionRepository;
        this.vitalsRepository = vitalsRepository;
        this.documentRepository = documentRepository;
        this.consentRepository = consentRepository;
        this.visitRepository = visitRepository;
        this.queueService = queueService;
        this.patientService = patientService;
    }

    @Transactional(readOnly = true)
    public PatientChartResponse getPatientChart(String patientId) {
        String appId = TenantContext.require();

        Patient patient = patientRepository.findByAppIdAndId(appId, patientId)
                .orElseThrow(() -> new NotFoundException("Patient record not found", "CHART_NOT_FOUND"));

        List<PatientAllergy> allergies = allergyRepository.findByAppIdAndPatientIdOrderByNotedAtDesc(appId, patientId);
        boolean hasSevere = allergies.stream().anyMatch(a -> a.getSeverity() == PatientAllergy.Severity.SEVERE);

        List<PatientCondition> conditions = conditionRepository.findByAppIdAndPatientIdOrderByCreatedAtDesc(appId, patientId);
        List<VisitVitals> vitals = vitalsRepository.findByAppIdAndPatientIdOrderByRecordedAtDesc(appId, patientId);
        List<PatientDocument> documents = documentRepository.findByAppIdAndPatientIdOrderByUploadedAtDesc(appId, patientId);
        List<ConsentForm> consents = consentRepository.findByAppIdAndPatientIdOrderBySignedAtDesc(appId, patientId);

        List<ClinicVisit> visits = visitRepository.findAllByAppIdAndPatientIdOrderByCreatedAtDesc(appId, patientId);
        List<ClinicVisitResponse> visitResponses = visits.stream()
                .limit(20)
                .map(queueService::toFullResponse)
                .collect(Collectors.toList());

        return new PatientChartResponse(
                patientService.toResponse(patient),
                allergies.stream().map(this::toAllergyDto).sorted(Comparator.comparing(PatientAllergyDto::severity).reversed()).collect(Collectors.toList()),
                hasSevere,
                conditions.stream().map(this::toConditionDto).collect(Collectors.toList()),
                vitals.stream().map(this::toVitalsDto).collect(Collectors.toList()),
                visitResponses,
                documents.stream().map(this::toDocumentDto).collect(Collectors.toList()),
                consents.stream().map(this::toConsentDto).collect(Collectors.toList())
        );
    }

    public PatientAllergyDto addAllergy(String patientId, AddAllergyRequest request) {
        String appId = TenantContext.require();

        patientRepository.findByAppIdAndId(appId, patientId)
                .orElseThrow(() -> new NotFoundException("Patient record not found", "PATIENT_NOT_FOUND"));

        if (request.substance() == null || request.substance().trim().isEmpty()) {
            throw new BusinessRuleException("Allergy substance is required", "ALLERGY_SUBSTANCE_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        PatientAllergy.Severity severity = PatientAllergy.Severity.MODERATE;
        if (request.severity() != null && !request.severity().trim().isEmpty()) {
            try {
                severity = PatientAllergy.Severity.valueOf(request.severity().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                severity = PatientAllergy.Severity.MODERATE;
            }
        }

        PatientAllergy allergy = new PatientAllergy(
                patientId,
                request.substance().trim(),
                severity,
                request.reactionNotes() != null ? request.reactionNotes().trim() : null
        );

        PatientAllergy saved = allergyRepository.save(allergy);
        log.info("Recorded allergy {} (severity: {}) for patient {} in tenant {}", saved.getSubstance(), saved.getSeverity(), patientId, appId);
        return toAllergyDto(saved);
    }

    public void deleteAllergy(String allergyId) {
        String appId = TenantContext.require();
        allergyRepository.findByAppIdAndId(appId, allergyId)
                .orElseThrow(() -> new NotFoundException("Allergy record not found", "CHART_NOT_FOUND"));
        allergyRepository.deleteByAppIdAndId(appId, allergyId);
    }

    public PatientConditionDto addCondition(String patientId, AddConditionRequest request) {
        String appId = TenantContext.require();

        patientRepository.findByAppIdAndId(appId, patientId)
                .orElseThrow(() -> new NotFoundException("Patient record not found", "PATIENT_NOT_FOUND"));

        if (request.label() == null || request.label().trim().isEmpty()) {
            throw new BusinessRuleException("Condition label is required", "CONDITION_LABEL_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        PatientCondition.Status status = PatientCondition.Status.ACTIVE;
        if (request.status() != null && !request.status().trim().isEmpty()) {
            try {
                status = PatientCondition.Status.valueOf(request.status().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                status = PatientCondition.Status.ACTIVE;
            }
        }

        PatientCondition condition = new PatientCondition(
                patientId,
                request.icdCode() != null ? request.icdCode().trim() : null,
                request.label().trim(),
                request.chronic(),
                request.onsetDate() != null ? request.onsetDate().trim() : null,
                status,
                request.notes() != null ? request.notes().trim() : null
        );

        PatientCondition saved = conditionRepository.save(condition);
        log.info("Recorded condition {} (chronic: {}) for patient {} in tenant {}", saved.getLabel(), saved.isChronic(), patientId, appId);
        return toConditionDto(saved);
    }

    public PatientConditionDto updateCondition(String conditionId, UpdateConditionRequest request) {
        String appId = TenantContext.require();
        PatientCondition condition = conditionRepository.findByAppIdAndId(appId, conditionId)
                .orElseThrow(() -> new NotFoundException("Condition record not found", "CHART_NOT_FOUND"));

        if (request.label() == null || request.label().trim().isEmpty()) {
            throw new BusinessRuleException("Condition label is required", "CONDITION_LABEL_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        condition.setIcdCode(request.icdCode() != null ? request.icdCode().trim() : null);
        condition.setLabel(request.label().trim());
        condition.setChronic(request.chronic());
        condition.setOnsetDate(request.onsetDate() != null ? request.onsetDate().trim() : null);
        if (request.status() != null && !request.status().trim().isEmpty()) {
            try {
                condition.setStatus(PatientCondition.Status.valueOf(request.status().trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                // keep current status
            }
        }
        condition.setNotes(request.notes() != null ? request.notes().trim() : null);

        PatientCondition saved = conditionRepository.save(condition);
        return toConditionDto(saved);
    }

    public void deleteCondition(String conditionId) {
        String appId = TenantContext.require();
        conditionRepository.findByAppIdAndId(appId, conditionId)
                .orElseThrow(() -> new NotFoundException("Condition record not found", "CHART_NOT_FOUND"));
        conditionRepository.deleteByAppIdAndId(appId, conditionId);
    }

    public VisitVitalsDto recordVitals(String visitId, RecordVitalsRequest request) {
        String appId = TenantContext.require();

        ClinicVisit visit = visitRepository.findByAppIdAndId(appId, visitId)
                .orElseThrow(() -> new NotFoundException("Clinic visit not found", "CLINIC_VISIT_NOT_FOUND"));

        validateVitalsRanges(request);

        VisitVitals vitals = vitalsRepository.findByAppIdAndVisitId(appId, visitId)
                .orElse(new VisitVitals(
                        visitId,
                        visit.getPatientId(),
                        request.systolicBp(),
                        request.diastolicBp(),
                        request.pulse(),
                        request.tempC(),
                        request.spo2(),
                        request.weightKg(),
                        request.heightCm(),
                        request.notes()
                ));

        vitals.setSystolicBp(request.systolicBp());
        vitals.setDiastolicBp(request.diastolicBp());
        vitals.setPulse(request.pulse());
        vitals.setTempC(request.tempC());
        vitals.setSpo2(request.spo2());
        vitals.setWeightKg(request.weightKg());
        vitals.setHeightCm(request.heightCm());
        vitals.setNotes(request.notes());
        vitals.setBmi(VisitVitals.calculateBmi(request.weightKg(), request.heightCm()));

        VisitVitals saved = vitalsRepository.save(vitals);
        log.info("Recorded vitals for visit {} (patient: {}, BMI: {}) in tenant {}", visitId, visit.getPatientId(), saved.getBmi(), appId);
        return toVitalsDto(saved);
    }

    public PatientDocumentDto saveDocument(String patientId, UploadDocumentRequest request) {
        String appId = TenantContext.require();

        patientRepository.findByAppIdAndId(appId, patientId)
                .orElseThrow(() -> new NotFoundException("Patient record not found", "PATIENT_NOT_FOUND"));

        if (request.fileName() == null || request.fileName().trim().isEmpty()) {
            throw new BusinessRuleException("Document file name is required", "DOCUMENT_FILENAME_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        PatientDocument.DocumentKind kind = PatientDocument.DocumentKind.REPORT;
        if (request.documentKind() != null && !request.documentKind().trim().isEmpty()) {
            try {
                kind = PatientDocument.DocumentKind.valueOf(request.documentKind().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                kind = PatientDocument.DocumentKind.REPORT;
            }
        }

        PatientDocument doc = new PatientDocument(
                patientId,
                request.visitId(),
                kind,
                request.fileName().trim(),
                request.contentType(),
                request.fileSize(),
                request.storagePath(),
                request.notes()
        );

        PatientDocument saved = documentRepository.save(doc);
        log.info("Archived patient document {} (kind: {}) for patient {} in tenant {}", saved.getFileName(), saved.getDocumentKind(), patientId, appId);
        return toDocumentDto(saved);
    }

    public void deleteDocument(String documentId) {
        String appId = TenantContext.require();
        documentRepository.findByAppIdAndId(appId, documentId)
                .orElseThrow(() -> new NotFoundException("Document record not found", "CHART_NOT_FOUND"));
        documentRepository.deleteByAppIdAndId(appId, documentId);
    }

    public ConsentFormDto signConsent(String patientId, SignConsentRequest request) {
        String appId = TenantContext.require();

        patientRepository.findByAppIdAndId(appId, patientId)
                .orElseThrow(() -> new NotFoundException("Patient record not found", "PATIENT_NOT_FOUND"));

        if (request.signedByName() == null || request.signedByName().trim().isEmpty()) {
            throw new BusinessRuleException("Signer name is required for consent", "CONSENT_SIGNED_BY_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (request.title() == null || request.title().trim().isEmpty()) {
            throw new BusinessRuleException("Consent title is required", "CONSENT_TITLE_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (request.bodyText() == null || request.bodyText().trim().isEmpty()) {
            throw new BusinessRuleException("Consent body text is required", "CONSENT_BODY_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        ConsentForm.Relation relation = ConsentForm.Relation.SELF;
        if (request.signedByRelation() != null && !request.signedByRelation().trim().isEmpty()) {
            try {
                relation = ConsentForm.Relation.valueOf(request.signedByRelation().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                relation = ConsentForm.Relation.SELF;
            }
        }

        ConsentForm consent = new ConsentForm(
                patientId,
                request.visitId(),
                request.templateKey() != null ? request.templateKey().trim() : "GENERAL_TREATMENT",
                request.title().trim(),
                request.bodyText().trim(),
                request.signedByName().trim(),
                relation,
                request.ipAddress()
        );

        ConsentForm saved = consentRepository.save(consent);
        log.info("Signed medical consent {} for patient {} by {} in tenant {}", saved.getTitle(), patientId, saved.getSignedByName(), appId);
        return toConsentDto(saved);
    }

    public void validateVitalsRanges(RecordVitalsRequest request) {
        if (request.systolicBp() != null && (request.systolicBp() < 60 || request.systolicBp() > 260)) {
            throw new BusinessRuleException("Systolic BP must be between 60 and 260 mmHg", "VITALS_RANGE_INVALID", HttpStatus.BAD_REQUEST);
        }
        if (request.diastolicBp() != null && (request.diastolicBp() < 40 || request.diastolicBp() > 160)) {
            throw new BusinessRuleException("Diastolic BP must be between 40 and 160 mmHg", "VITALS_RANGE_INVALID", HttpStatus.BAD_REQUEST);
        }
        if (request.pulse() != null && (request.pulse() < 30 || request.pulse() > 220)) {
            throw new BusinessRuleException("Pulse must be between 30 and 220 bpm", "VITALS_RANGE_INVALID", HttpStatus.BAD_REQUEST);
        }
        if (request.tempC() != null && (request.tempC().compareTo(new BigDecimal("34.0")) < 0 || request.tempC().compareTo(new BigDecimal("43.0")) > 0)) {
            throw new BusinessRuleException("Temperature must be between 34.0 and 43.0 °C", "VITALS_RANGE_INVALID", HttpStatus.BAD_REQUEST);
        }
        if (request.spo2() != null && (request.spo2() < 50 || request.spo2() > 100)) {
            throw new BusinessRuleException("SpO2 must be between 50% and 100%", "VITALS_RANGE_INVALID", HttpStatus.BAD_REQUEST);
        }
        if (request.weightKg() != null && (request.weightKg().compareTo(new BigDecimal("1.0")) < 0 || request.weightKg().compareTo(new BigDecimal("400.0")) > 0)) {
            throw new BusinessRuleException("Weight must be between 1.0 and 400.0 kg", "VITALS_RANGE_INVALID", HttpStatus.BAD_REQUEST);
        }
        if (request.heightCm() != null && (request.heightCm().compareTo(new BigDecimal("30.0")) < 0 || request.heightCm().compareTo(new BigDecimal("250.0")) > 0)) {
            throw new BusinessRuleException("Height must be between 30.0 and 250.0 cm", "VITALS_RANGE_INVALID", HttpStatus.BAD_REQUEST);
        }
    }

    private PatientAllergyDto toAllergyDto(PatientAllergy a) {
        return new PatientAllergyDto(
                a.getId(),
                a.getPatientId(),
                a.getSubstance(),
                a.getSeverity().name(),
                a.getReactionNotes(),
                a.getNotedAt()
        );
    }

    private PatientConditionDto toConditionDto(PatientCondition c) {
        return new PatientConditionDto(
                c.getId(),
                c.getPatientId(),
                c.getIcdCode(),
                c.getLabel(),
                c.isChronic(),
                c.getOnsetDate(),
                c.getStatus().name(),
                c.getNotes(),
                c.getCreatedAt()
        );
    }

    private VisitVitalsDto toVitalsDto(VisitVitals v) {
        return new VisitVitalsDto(
                v.getId(),
                v.getVisitId(),
                v.getPatientId(),
                v.getSystolicBp(),
                v.getDiastolicBp(),
                v.getPulse(),
                v.getTempC(),
                v.getSpo2(),
                v.getWeightKg(),
                v.getHeightCm(),
                v.getBmi(),
                v.getNotes(),
                v.getRecordedAt()
        );
    }

    private PatientDocumentDto toDocumentDto(PatientDocument d) {
        return new PatientDocumentDto(
                d.getId(),
                d.getPatientId(),
                d.getVisitId(),
                d.getDocumentKind().name(),
                d.getFileName(),
                d.getContentType(),
                d.getFileSize(),
                d.getStoragePath(),
                d.getNotes(),
                d.getUploadedAt()
        );
    }

    private ConsentFormDto toConsentDto(ConsentForm c) {
        return new ConsentFormDto(
                c.getId(),
                c.getPatientId(),
                c.getVisitId(),
                c.getTemplateKey(),
                c.getTitle(),
                c.getBodyText(),
                c.getSignedByName(),
                c.getSignedByRelation().name(),
                c.getSignedAt(),
                c.getIpAddress()
        );
    }
}
