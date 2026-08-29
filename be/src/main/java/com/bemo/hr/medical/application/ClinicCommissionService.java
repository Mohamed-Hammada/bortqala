package com.bemo.hr.medical.application;

import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.medical.api.MedicalClinicApi.ClinicVisitResponse;
import com.bemo.hr.medical.api.MedicalClinicApi.DoctorCommissionStatementResponse;
import com.bemo.hr.medical.domain.ClinicVisit;
import com.bemo.hr.medical.domain.Patient;
import com.bemo.hr.medical.infrastructure.ClinicVisitRepository;
import com.bemo.hr.medical.infrastructure.PatientRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ClinicCommissionService {

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("50.00");

    private final ClinicVisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final EmployeeRepository employeeRepository;
    private final ClinicPrescriptionService prescriptionService;

    public ClinicCommissionService(ClinicVisitRepository visitRepository,
                                   PatientRepository patientRepository,
                                   EmployeeRepository employeeRepository,
                                   ClinicPrescriptionService prescriptionService) {
        this.visitRepository = visitRepository;
        this.patientRepository = patientRepository;
        this.employeeRepository = employeeRepository;
        this.prescriptionService = prescriptionService;
    }

    public DoctorCommissionStatementResponse getMonthlyStatement(String doctorEmployeeId, int year, int month, BigDecimal customRate) {
        String appId = TenantContext.require();

        if (doctorEmployeeId == null || doctorEmployeeId.trim().isEmpty()) {
            throw new BusinessRuleException("Doctor is required", "CLINIC_DOCTOR_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (year < 2000 || year > 2100 || month < 1 || month > 12) {
            throw new BusinessRuleException("Invalid doctor commission period", "CLINIC_COMMISSION_INVALID_PERIOD", HttpStatus.BAD_REQUEST);
        }

        String periodPrefix = String.format("%04d-%02d", year, month);
        List<ClinicVisit> completedVisits = visitRepository.findCompletedVisitsForDoctorInPeriod(appId, doctorEmployeeId.trim(), periodPrefix);

        String doctorName = employeeRepository.findById(doctorEmployeeId.trim())
                .map(com.bemo.hr.employee.domain.Employee::getFullName)
                .orElse(doctorEmployeeId.trim());

        BigDecimal commissionRate = customRate != null && customRate.compareTo(BigDecimal.ZERO) >= 0
                ? customRate
                : DEFAULT_COMMISSION_RATE;

        BigDecimal totalRevenue = completedVisits.stream()
                .map(ClinicVisit::getFeeCharged)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal commissionAmount = totalRevenue.multiply(commissionRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        Map<String, Patient> patientMap = patientRepository.findAllById(
                completedVisits.stream().map(ClinicVisit::getPatientId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Patient::getId, p -> p));

        List<ClinicVisitResponse> visitResponses = completedVisits.stream().map(v -> {
            Patient p = patientMap.get(v.getPatientId());
            return new ClinicVisitResponse(
                    v.getId(),
                    v.getPatientId(),
                    p != null ? p.getFullName() : "Unknown",
                    p != null ? p.getMrn() : "",
                    p != null ? p.getPhone() : "",
                    v.getDoctorEmployeeId(),
                    doctorName,
                    v.getVisitDate(),
                    v.getVisitTime().toEpochMilli(),
                    v.getToken(),
                    v.getStatus().name(),
                    v.getChiefComplaint(),
                    v.getDiagnosisIcd(),
                    v.getDiagnosisNotes(),
                    v.getFeeCharged(),
                    v.getInsuranceCovered(),
                    v.getPatientShare(),
                    v.getPaymentMethod(),
                    prescriptionService.getPrescriptions(v.getId()),
                    v.getCreatedAt().toEpochMilli(),
                    v.getUpdatedAt().toEpochMilli()
            );
        }).toList();

        return new DoctorCommissionStatementResponse(
                doctorEmployeeId.trim(),
                doctorName,
                periodPrefix,
                completedVisits.size(),
                totalRevenue,
                commissionRate,
                commissionAmount,
                visitResponses
        );
    }
}
