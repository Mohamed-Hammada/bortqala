package com.bemo.hr.medical.application;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.domain.MedicalLicenseRecord;
import com.bemo.hr.medical.domain.PatientFamilyLink;
import com.bemo.hr.medical.domain.TelemedicineSession;
import com.bemo.hr.medical.infrastructure.MedicalLicenseRecordRepository;
import com.bemo.hr.medical.infrastructure.PatientFamilyLinkRepository;
import com.bemo.hr.medical.infrastructure.PatientRepository;
import com.bemo.hr.medical.infrastructure.TelemedicineSessionRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MedicalDepthService {

    private final PatientFamilyLinkRepository familyLinkRepository;
    private final TelemedicineSessionRepository telemedRepository;
    private final MedicalLicenseRecordRepository licenseRepository;
    private final PatientRepository patientRepository;

    public MedicalDepthService(
            PatientFamilyLinkRepository familyLinkRepository,
            TelemedicineSessionRepository telemedRepository,
            MedicalLicenseRecordRepository licenseRepository,
            PatientRepository patientRepository) {
        this.familyLinkRepository = familyLinkRepository;
        this.telemedRepository = telemedRepository;
        this.licenseRepository = licenseRepository;
        this.patientRepository = patientRepository;
    }

    public PatientFamilyLinkDto linkFamilyMember(String patientId, LinkFamilyMemberRequest req) {
        String tenantId = TenantContext.require();

        if (patientId.equals(req.guardianPatientId())) {
            throw new BusinessRuleException("Cannot link patient to self as guardian.", "FAMILY_LINK_SELF_REFERENCE", HttpStatus.BAD_REQUEST);
        }

        patientRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("PATIENT_NOT_FOUND", "Patient not found: " + patientId));
        patientRepository.findById(req.guardianPatientId())
                .orElseThrow(() -> new NotFoundException("PATIENT_NOT_FOUND", "Guardian patient not found: " + req.guardianPatientId()));

        if (familyLinkRepository.findByPatientIdAndGuardianPatientId(patientId, req.guardianPatientId()).isPresent()) {
            throw new BusinessRuleException("Family link already exists.", "FAMILY_LINK_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        PatientFamilyLink link = new PatientFamilyLink(
                UUID.randomUUID().toString(),
                tenantId,
                patientId,
                req.guardianPatientId(),
                req.relationshipType(),
                req.isPrimaryPayer(),
                req.notes()
        );

        link = familyLinkRepository.save(link);
        return mapToFamilyDto(link);
    }

    @Transactional(readOnly = true)
    public List<PatientFamilyLinkDto> getFamilyLinks(String patientId) {
        return familyLinkRepository.findByPatientId(patientId)
                .stream()
                .map(this::mapToFamilyDto)
                .toList();
    }

    public PediatricDoseCalculationResponse calculatePediatricDose(PediatricDoseCalculationRequest req) {
        if (req.frequencyPerDay() <= 0) {
            throw new BusinessRuleException("Frequency must be greater than 0.", "INVALID_DOSE_FREQUENCY", HttpStatus.BAD_REQUEST);
        }

        BigDecimal dailyDoseMg = req.weightKg().multiply(req.doseMgPerKgPerDay()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal singleDoseMg = dailyDoseMg.divide(BigDecimal.valueOf(req.frequencyPerDay()), 2, RoundingMode.HALF_UP);
        BigDecimal singleDoseMl = null;

        if (req.drugConcentrationMgPerMl() != null && req.drugConcentrationMgPerMl().compareTo(BigDecimal.ZERO) > 0) {
            singleDoseMl = singleDoseMg.divide(req.drugConcentrationMgPerMl(), 2, RoundingMode.HALF_UP);
        }

        String instructions = String.format("Administer %.2f mg (%s) %d times daily",
                singleDoseMg,
                singleDoseMl != null ? String.format("%.2f ml", singleDoseMl) : "per prescribed form",
                req.frequencyPerDay()
        );

        return new PediatricDoseCalculationResponse(
                req.weightKg(),
                dailyDoseMg,
                singleDoseMg,
                singleDoseMl,
                req.frequencyPerDay(),
                instructions
        );
    }

    public TelemedicineSessionDto scheduleTelemedicineSession(ScheduleTelemedicineSessionRequest req) {
        String tenantId = TenantContext.require();
        patientRepository.findById(req.patientId())
                .orElseThrow(() -> new NotFoundException("PATIENT_NOT_FOUND", "Patient not found: " + req.patientId()));

        String roomName = (req.roomName() != null && !req.roomName().isBlank())
                ? req.roomName().strip()
                : "bemo-room-" + UUID.randomUUID().toString().substring(0, 8);
        String meetingLink = "https://meet.jit.si/" + roomName;

        TelemedicineSession session = new TelemedicineSession(
                UUID.randomUUID().toString(),
                tenantId,
                req.patientId(),
                req.doctorId(),
                req.doctorName(),
                req.scheduledTime() > 0 ? req.scheduledTime() : System.currentTimeMillis(),
                meetingLink,
                roomName
        );

        session = telemedRepository.save(session);
        return mapToTelemedDto(session);
    }

    @Transactional(readOnly = true)
    public List<TelemedicineSessionDto> getTelemedicineSessionsByPatient(String patientId) {
        return telemedRepository.findByPatientIdOrderByScheduledTimeDesc(patientId)
                .stream()
                .map(this::mapToTelemedDto)
                .toList();
    }

    public MedicalLicenseRecordDto registerLicense(RegisterMedicalLicenseRequest req) {
        String tenantId = TenantContext.require();

        MedicalLicenseRecord lic = new MedicalLicenseRecord(
                UUID.randomUUID().toString(),
                tenantId,
                req.practitionerId(),
                req.practitionerName(),
                req.licenseType(),
                req.licenseNumber(),
                req.issuingAuthority(),
                req.issueDate() > 0 ? req.issueDate() : System.currentTimeMillis(),
                req.expiryDate()
        );

        lic = licenseRepository.save(lic);
        return mapToLicenseDto(lic);
    }

    @Transactional(readOnly = true)
    public List<MedicalLicenseRecordDto> getAllLicenses() {
        return licenseRepository.findAllByOrderByExpiryDateAsc()
                .stream()
                .map(this::mapToLicenseDto)
                .toList();
    }

    private PatientFamilyLinkDto mapToFamilyDto(PatientFamilyLink l) {
        return new PatientFamilyLinkDto(
                l.getId(),
                l.getPatientId(),
                l.getGuardianPatientId(),
                l.getRelationshipType(),
                l.isPrimaryPayer(),
                l.getNotes(),
                l.getCreatedAt()
        );
    }

    private TelemedicineSessionDto mapToTelemedDto(TelemedicineSession s) {
        return new TelemedicineSessionDto(
                s.getId(),
                s.getPatientId(),
                s.getDoctorId(),
                s.getDoctorName(),
                s.getScheduledTime(),
                s.getMeetingLink(),
                s.getRoomToken(),
                s.getStatus(),
                s.getClinicalNotes(),
                s.getCreatedAt()
        );
    }

    private MedicalLicenseRecordDto mapToLicenseDto(MedicalLicenseRecord l) {
        return new MedicalLicenseRecordDto(
                l.getId(),
                l.getPractitionerId(),
                l.getPractitionerName(),
                l.getLicenseType(),
                l.getLicenseNumber(),
                l.getIssuingAuthority(),
                l.getIssueDate(),
                l.getExpiryDate(),
                l.getStatus(),
                l.getCreatedAt()
        );
    }
}
