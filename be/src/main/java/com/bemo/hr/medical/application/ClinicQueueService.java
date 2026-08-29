package com.bemo.hr.medical.application;

import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.domain.ClinicVisit;
import com.bemo.hr.medical.domain.Patient;
import com.bemo.hr.medical.infrastructure.ClinicVisitRepository;
import com.bemo.hr.medical.infrastructure.PatientRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ClinicQueueService {

    private final ClinicVisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final EmployeeRepository employeeRepository;
    private final ClinicPrescriptionService prescriptionService;

    public ClinicQueueService(ClinicVisitRepository visitRepository,
                              PatientRepository patientRepository,
                              EmployeeRepository employeeRepository,
                              ClinicPrescriptionService prescriptionService) {
        this.visitRepository = visitRepository;
        this.patientRepository = patientRepository;
        this.employeeRepository = employeeRepository;
        this.prescriptionService = prescriptionService;
    }

    public ClinicVisitResponse queueVisit(QueueVisitRequest request) {
        String appId = TenantContext.require();

        if (request.patientId() == null || request.patientId().trim().isEmpty()) {
            throw new BusinessRuleException("Patient ID is required", "PATIENT_NOT_FOUND", HttpStatus.BAD_REQUEST);
        }
        if (request.doctorEmployeeId() == null || request.doctorEmployeeId().trim().isEmpty()) {
            throw new BusinessRuleException("Doctor is required", "CLINIC_DOCTOR_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        Patient patient = patientRepository.findByAppIdAndId(appId, request.patientId())
                .orElseThrow(() -> new NotFoundException("Patient record not found", "PATIENT_NOT_FOUND"));

        String visitDate = request.visitDate() != null && !request.visitDate().trim().isEmpty()
                ? request.visitDate().trim()
                : LocalDate.now().toString();

        Integer maxToken = visitRepository.findMaxTokenForDoctorAndDate(appId, request.doctorEmployeeId(), visitDate);
        int nextToken = (maxToken != null ? maxToken : 0) + 1;

        ClinicVisit visit = new ClinicVisit(
                patient.getId(),
                request.doctorEmployeeId(),
                visitDate,
                nextToken,
                request.feeCharged(),
                request.insuranceCovered(),
                request.paymentMethod()
        );

        ClinicVisit saved = visitRepository.save(visit);
        log.info("Queued visit token {} for patient {} with doctor {} on {}", nextToken, patient.getMrn(), request.doctorEmployeeId(), visitDate);
        return toResponse(saved, patient, null, List.of());
    }

    public ClinicVisitResponse callVisit(String visitId) {
        String appId = TenantContext.require();
        ClinicVisit visit = visitRepository.findByAppIdAndId(appId, visitId)
                .orElseThrow(() -> new NotFoundException("Clinic visit not found", "CLINIC_VISIT_NOT_FOUND"));

        if (visit.getStatus() == ClinicVisit.Status.DONE) {
            throw new BusinessRuleException("Clinic visit is already completed", "CLINIC_VISIT_ALREADY_COMPLETED", HttpStatus.CONFLICT);
        }
        if (visit.getStatus() == ClinicVisit.Status.CANCELLED) {
            throw new BusinessRuleException("Clinic visit is already cancelled", "CLINIC_VISIT_ALREADY_CANCELLED", HttpStatus.CONFLICT);
        }

        visit.callToRoom();
        ClinicVisit saved = visitRepository.save(visit);
        return toFullResponse(saved);
    }

    public ClinicVisitResponse completeVisit(String visitId, CompleteVisitRequest request) {
        String appId = TenantContext.require();
        ClinicVisit visit = visitRepository.findByAppIdAndId(appId, visitId)
                .orElseThrow(() -> new NotFoundException("Clinic visit not found", "CLINIC_VISIT_NOT_FOUND"));

        if (visit.getStatus() == ClinicVisit.Status.CANCELLED) {
            throw new BusinessRuleException("Clinic visit is already cancelled", "CLINIC_VISIT_ALREADY_CANCELLED", HttpStatus.CONFLICT);
        }

        visit.complete(
                request.chiefComplaint(),
                request.diagnosisIcd(),
                request.diagnosisNotes(),
                request.feeCharged(),
                request.insuranceCovered(),
                request.paymentMethod()
        );

        ClinicVisit saved = visitRepository.save(visit);

        List<PrescriptionLineResponse> rxLines = List.of();
        if (request.prescriptionLines() != null && !request.prescriptionLines().isEmpty()) {
            rxLines = prescriptionService.savePrescriptions(saved.getId(), request.prescriptionLines());
        }

        return toFullResponse(saved, rxLines);
    }

    public ClinicVisitResponse cancelVisit(String visitId) {
        String appId = TenantContext.require();
        ClinicVisit visit = visitRepository.findByAppIdAndId(appId, visitId)
                .orElseThrow(() -> new NotFoundException("Clinic visit not found", "CLINIC_VISIT_NOT_FOUND"));

        if (visit.getStatus() == ClinicVisit.Status.DONE) {
            throw new BusinessRuleException("Clinic visit is already completed", "CLINIC_VISIT_ALREADY_COMPLETED", HttpStatus.CONFLICT);
        }

        visit.cancel();
        ClinicVisit saved = visitRepository.save(visit);
        return toFullResponse(saved);
    }

    @Transactional(readOnly = true)
    public ClinicVisitResponse getVisit(String visitId) {
        String appId = TenantContext.require();
        ClinicVisit visit = visitRepository.findByAppIdAndId(appId, visitId)
                .orElseThrow(() -> new NotFoundException("Clinic visit not found", "CLINIC_VISIT_NOT_FOUND"));
        return toFullResponse(visit);
    }

    @Transactional(readOnly = true)
    public List<ClinicVisitResponse> getQueueForDate(String date, String doctorEmployeeId) {
        String appId = TenantContext.require();
        String targetDate = date != null && !date.trim().isEmpty() ? date.trim() : LocalDate.now().toString();

        List<ClinicVisit> visits;
        if (doctorEmployeeId != null && !doctorEmployeeId.trim().isEmpty()) {
            visits = visitRepository.findAllByAppIdAndDoctorEmployeeIdAndVisitDateOrderByTokenAsc(appId, doctorEmployeeId.trim(), targetDate);
        } else {
            visits = visitRepository.findAllByAppIdAndVisitDateOrderByTokenAsc(appId, targetDate);
        }

        if (visits.isEmpty()) {
            return List.of();
        }

        Map<String, Patient> patientMap = patientRepository.findAllById(visits.stream().map(ClinicVisit::getPatientId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Patient::getId, p -> p));

        return visits.stream().map(v -> {
            Patient p = patientMap.get(v.getPatientId());
            List<PrescriptionLineResponse> rx = prescriptionService.getPrescriptions(v.getId());
            return toResponse(v, p, null, rx);
        }).toList();
    }

    public ClinicVisitResponse toFullResponse(ClinicVisit visit) {
        return toFullResponse(visit, prescriptionService.getPrescriptions(visit.getId()));
    }

    private ClinicVisitResponse toFullResponse(ClinicVisit visit, List<PrescriptionLineResponse> rxLines) {
        String appId = TenantContext.require();
        Patient patient = patientRepository.findByAppIdAndId(appId, visit.getPatientId()).orElse(null);
        String doctorName = employeeRepository.findById(visit.getDoctorEmployeeId())
                .map(com.bemo.hr.employee.domain.Employee::getFullName)
                .orElse(visit.getDoctorEmployeeId());
        return toResponse(visit, patient, doctorName, rxLines);
    }

    public ClinicVisitResponse toResponse(ClinicVisit visit, Patient patient, String doctorName, List<PrescriptionLineResponse> rxLines) {
        String pName = patient != null ? patient.getFullName() : "Unknown";
        String pMrn = patient != null ? patient.getMrn() : "";
        String pPhone = patient != null ? patient.getPhone() : "";
        String docName = doctorName != null ? doctorName : visit.getDoctorEmployeeId();

        return new ClinicVisitResponse(
                visit.getId(),
                visit.getPatientId(),
                pName,
                pMrn,
                pPhone,
                visit.getDoctorEmployeeId(),
                docName,
                visit.getVisitDate(),
                visit.getVisitTime().toEpochMilli(),
                visit.getToken(),
                visit.getStatus().name(),
                visit.getChiefComplaint(),
                visit.getDiagnosisIcd(),
                visit.getDiagnosisNotes(),
                visit.getFeeCharged(),
                visit.getInsuranceCovered(),
                visit.getPatientShare(),
                visit.getPaymentMethod(),
                rxLines != null ? rxLines : List.of(),
                visit.getCreatedAt().toEpochMilli(),
                visit.getUpdatedAt().toEpochMilli()
        );
    }
}
