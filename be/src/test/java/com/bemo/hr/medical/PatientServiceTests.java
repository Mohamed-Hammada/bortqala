package com.bemo.hr.medical;

import com.bemo.hr.medical.api.MedicalClinicApi.DuplicateCheckResponse;
import com.bemo.hr.medical.api.MedicalClinicApi.PatientResponse;
import com.bemo.hr.medical.api.MedicalClinicApi.RegisterPatientRequest;
import com.bemo.hr.medical.application.PatientService;
import com.bemo.hr.medical.domain.Patient;
import com.bemo.hr.medical.domain.PatientMrnSequence;
import com.bemo.hr.medical.infrastructure.PatientMrnSequenceRepository;
import com.bemo.hr.medical.infrastructure.PatientRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PatientServiceTests {

    private PatientRepository patientRepository;
    private PatientMrnSequenceRepository sequenceRepository;
    private PatientService patientService;

    @BeforeEach
    void setUp() {
        TenantContext.set("app-clinic-1");
        patientRepository = mock(PatientRepository.class);
        sequenceRepository = mock(PatientMrnSequenceRepository.class);
        patientService = new PatientService(patientRepository, sequenceRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Register patient generates MRN-00001 and parses National ID")
    void testRegisterPatientSuccess() {
        PatientMrnSequence seq = new PatientMrnSequence("app-clinic-1", 0L);
        when(sequenceRepository.findByAppIdForUpdate("app-clinic-1")).thenReturn(Optional.of(seq));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterPatientRequest request = new RegisterPatientRequest(
                "29008200101534", // 1990-08-20, Cairo, index 12 is '3' -> MALE
                "Ahmed Mohamed",
                "01012345678",
                null, // will be auto-derived
                null, // will be auto-derived
                "O_POSITIVE",
                "Penicillin",
                "Hypertension",
                "Mona Ahmed",
                "01098765432"
        );

        PatientResponse response = patientService.registerPatient(request);

        assertNotNull(response);
        assertEquals("MRN-00001", response.mrn());
        assertEquals("Ahmed Mohamed", response.fullName());
        assertEquals("01012345678", response.phone());
        assertEquals("MALE", response.gender());
        assertEquals("1990-08-20", response.birthDate());
        assertEquals("O_POSITIVE", response.bloodGroup());
        assertEquals("Penicillin", response.allergiesText());

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(captor.capture());
        assertEquals("MRN-00001", captor.getValue().getMrn());
    }

    @Test
    @DisplayName("Register patient with invalid National ID throws PATIENT_NATIONAL_ID_INVALID")
    void testRegisterInvalidNationalId() {
        RegisterPatientRequest request = new RegisterPatientRequest(
                "12345", // invalid
                "Ahmed Mohamed",
                "01012345678",
                "MALE",
                "1990-01-01",
                null, null, null, null, null
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> patientService.registerPatient(request));
        assertEquals("PATIENT_NATIONAL_ID_INVALID", ex.getCode());
    }

    @Test
    @DisplayName("Duplicate check finds patient by phone or national ID")
    void testDuplicateCheck() {
        Patient existing = new Patient("MRN-00005", "29008200101534", "Existing Patient", "01012345678", "MALE", "1990-08-20", null, null, null, null, null);
        when(patientRepository.findAllByAppIdAndPhone("app-clinic-1", "01012345678")).thenReturn(List.of(existing));

        DuplicateCheckResponse response = patientService.checkDuplicates("01012345678", null);
        assertTrue(response.duplicateFound());
        assertEquals(1, response.matchingPatients().size());
        assertEquals("MRN-00005", response.matchingPatients().get(0).mrn());
    }
}
