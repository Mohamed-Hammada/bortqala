package com.bemo.hr.medical.application;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.domain.Patient;
import com.bemo.hr.medical.domain.PatientMrnSequence;
import com.bemo.hr.medical.infrastructure.PatientMrnSequenceRepository;
import com.bemo.hr.medical.infrastructure.PatientRepository;
import com.bemo.hr.medical.nationalid.EgyptianNationalIdParser;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMrnSequenceRepository sequenceRepository;

    public PatientService(PatientRepository patientRepository,
                          PatientMrnSequenceRepository sequenceRepository) {
        this.patientRepository = patientRepository;
        this.sequenceRepository = sequenceRepository;
    }

    public PatientResponse registerPatient(RegisterPatientRequest request) {
        String appId = TenantContext.require();

        if (request.fullName() == null || request.fullName().trim().isEmpty()) {
            throw new BusinessRuleException("Patient full name is required", "PATIENT_NAME_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (request.phone() == null || request.phone().trim().isEmpty()) {
            throw new BusinessRuleException("Patient phone number is required", "PATIENT_PHONE_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        String birthDate = request.birthDate();
        String gender = request.gender();

        String nationalId = request.nationalId();
        if (nationalId != null && !nationalId.trim().isEmpty()) {
            nationalId = nationalId.trim();
            EgyptianNationalIdParser.ParseResult parseResult = EgyptianNationalIdParser.parse(nationalId);
            if (!parseResult.valid()) {
                throw new BusinessRuleException("Egyptian national ID is invalid: " + parseResult.errorMessage(), "PATIENT_NATIONAL_ID_INVALID", HttpStatus.BAD_REQUEST);
            }
            if (birthDate == null || birthDate.trim().isEmpty()) {
                birthDate = parseResult.birthDate().toString();
            }
            if (gender == null || gender.trim().isEmpty()) {
                gender = parseResult.gender();
            }
        }

        if (gender == null || gender.trim().isEmpty()) {
            gender = "UNKNOWN";
        }

        String mrn = generateNextMrn(appId);

        Patient patient = new Patient(
                mrn,
                nationalId,
                request.fullName().trim(),
                request.phone().trim(),
                gender,
                birthDate,
                request.bloodGroup(),
                request.allergiesText(),
                request.notes(),
                request.emergencyContactName(),
                request.emergencyContactPhone()
        );

        Patient saved = patientRepository.save(patient);
        log.info("Registered patient MRN {} (ID: {}) for tenant {}", saved.getMrn(), saved.getId(), appId);
        return toResponse(saved);
    }

    public PatientResponse updatePatient(String id, UpdatePatientRequest request) {
        String appId = TenantContext.require();
        Patient patient = patientRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new NotFoundException("Patient record not found", "PATIENT_NOT_FOUND"));

        if (request.fullName() == null || request.fullName().trim().isEmpty()) {
            throw new BusinessRuleException("Patient full name is required", "PATIENT_NAME_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (request.phone() == null || request.phone().trim().isEmpty()) {
            throw new BusinessRuleException("Patient phone number is required", "PATIENT_PHONE_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        String nationalId = request.nationalId();
        String birthDate = request.birthDate();
        String gender = request.gender();

        if (nationalId != null && !nationalId.trim().isEmpty()) {
            nationalId = nationalId.trim();
            EgyptianNationalIdParser.ParseResult parseResult = EgyptianNationalIdParser.parse(nationalId);
            if (!parseResult.valid()) {
                throw new BusinessRuleException("Egyptian national ID is invalid: " + parseResult.errorMessage(), "PATIENT_NATIONAL_ID_INVALID", HttpStatus.BAD_REQUEST);
            }
            if (birthDate == null || birthDate.trim().isEmpty()) {
                birthDate = parseResult.birthDate().toString();
            }
            if (gender == null || gender.trim().isEmpty()) {
                gender = parseResult.gender();
            }
        }

        patient.setFullName(request.fullName().trim());
        patient.setPhone(request.phone().trim());
        patient.setNationalId(nationalId);
        patient.setGender(gender != null ? gender : patient.getGender());
        patient.setBirthDate(birthDate);
        patient.setBloodGroup(request.bloodGroup());
        patient.setAllergiesText(request.allergiesText());
        patient.setNotes(request.notes());
        patient.setEmergencyContactName(request.emergencyContactName());
        patient.setEmergencyContactPhone(request.emergencyContactPhone());

        Patient saved = patientRepository.save(patient);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatient(String id) {
        String appId = TenantContext.require();
        Patient patient = patientRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new NotFoundException("Patient record not found", "PATIENT_NOT_FOUND"));
        return toResponse(patient);
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> searchPatients(String query, Pageable pageable) {
        String appId = TenantContext.require();
        if (query == null || query.trim().isEmpty()) {
            return patientRepository.findAllByAppIdOrderByCreatedAtDesc(appId, pageable).map(this::toResponse);
        }
        return patientRepository.searchPatients(appId, query.trim(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DuplicateCheckResponse checkDuplicates(String phone, String nationalId) {
        String appId = TenantContext.require();
        List<Patient> matches = new ArrayList<>();

        if (phone != null && !phone.trim().isEmpty()) {
            matches.addAll(patientRepository.findAllByAppIdAndPhone(appId, phone.trim()));
        }
        if (nationalId != null && !nationalId.trim().isEmpty()) {
            patientRepository.findByAppIdAndNationalId(appId, nationalId.trim())
                    .ifPresent(p -> {
                        if (matches.stream().noneMatch(m -> m.getId().equals(p.getId()))) {
                            matches.add(p);
                        }
                    });
        }

        List<PatientResponse> responses = matches.stream().map(this::toResponse).toList();
        return new DuplicateCheckResponse(!responses.isEmpty(), responses);
    }

    private synchronized String generateNextMrn(String appId) {
        PatientMrnSequence sequence = sequenceRepository.findByAppIdForUpdate(appId)
                .orElseGet(() -> sequenceRepository.save(new PatientMrnSequence(appId, 0L)));

        long next = sequence.next();
        sequenceRepository.save(sequence);
        return String.format("MRN-%05d", next);
    }

    public PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getMrn(),
                patient.getNationalId(),
                patient.getFullName(),
                patient.getPhone(),
                patient.getGender(),
                patient.getBirthDate(),
                patient.getBloodGroup(),
                patient.getAllergiesText(),
                patient.getNotes(),
                patient.getEmergencyContactName(),
                patient.getEmergencyContactPhone(),
                patient.getCreatedAt().toEpochMilli(),
                patient.getUpdatedAt().toEpochMilli()
        );
    }
}
