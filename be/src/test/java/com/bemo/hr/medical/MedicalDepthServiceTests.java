package com.bemo.hr.medical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.MedicalDepthService;
import com.bemo.hr.medical.domain.MedicalLicenseRecord;
import com.bemo.hr.medical.domain.Patient;
import com.bemo.hr.medical.domain.PatientFamilyLink;
import com.bemo.hr.medical.domain.TelemedicineSession;
import com.bemo.hr.medical.infrastructure.MedicalLicenseRecordRepository;
import com.bemo.hr.medical.infrastructure.PatientFamilyLinkRepository;
import com.bemo.hr.medical.infrastructure.PatientRepository;
import com.bemo.hr.medical.infrastructure.TelemedicineSessionRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedicalDepthServiceTests {

    @Mock
    private PatientFamilyLinkRepository familyLinkRepository;

    @Mock
    private TelemedicineSessionRepository telemedRepository;

    @Mock
    private MedicalLicenseRecordRepository licenseRepository;

    @Mock
    private PatientRepository patientRepository;

    private MedicalDepthService service;

    @BeforeEach
    void setUp() {
        TenantContext.set("app-test");
        service = new MedicalDepthService(familyLinkRepository, telemedRepository, licenseRepository, patientRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void linkFamilyMemberSuccessfully() {
        Patient child = new Patient("MRN-001", null, "Adam Amr", "01001", "MALE", "2020-01-01", null, null, null, null, null);
        Patient parent = new Patient("MRN-002", null, "Amr Hassan", "01002", "MALE", "1990-01-01", null, null, null, null, null);

        when(patientRepository.findById("PAT-CHILD")).thenReturn(Optional.of(child));
        when(patientRepository.findById("PAT-PARENT")).thenReturn(Optional.of(parent));
        when(familyLinkRepository.findByPatientIdAndGuardianPatientId("PAT-CHILD", "PAT-PARENT")).thenReturn(Optional.empty());
        when(familyLinkRepository.save(any(PatientFamilyLink.class))).thenAnswer(inv -> inv.getArgument(0));

        LinkFamilyMemberRequest req = new LinkFamilyMemberRequest("PAT-PARENT", "PARENT", true, "Father and primary insurance payer");
        PatientFamilyLinkDto dto = service.linkFamilyMember("PAT-CHILD", req);

        assertThat(dto.patientId()).isEqualTo("PAT-CHILD");
        assertThat(dto.guardianPatientId()).isEqualTo("PAT-PARENT");
        assertThat(dto.relationshipType()).isEqualTo("PARENT");
        assertThat(dto.isPrimaryPayer()).isTrue();
    }

    @Test
    void linkFamilyMemberSelfReferenceThrows() {
        LinkFamilyMemberRequest req = new LinkFamilyMemberRequest("PAT-1", "GUARDIAN", false, null);

        assertThatThrownBy(() -> service.linkFamilyMember("PAT-1", req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot link patient to self");
    }

    @Test
    void calculatePediatricDoseAccurately() {
        // Child weight: 15 kg, dose: 40 mg/kg/day (600 mg/day), 3 times daily (200 mg/dose), concentration: 50 mg/ml (4 ml/dose)
        PediatricDoseCalculationRequest req = new PediatricDoseCalculationRequest(
                BigDecimal.valueOf(15),
                BigDecimal.valueOf(40),
                3,
                BigDecimal.valueOf(50)
        );

        PediatricDoseCalculationResponse res = service.calculatePediatricDose(req);
        assertThat(res.dailyDoseMg()).isEqualByComparingTo("600.00");
        assertThat(res.singleDoseMg()).isEqualByComparingTo("200.00");
        assertThat(res.singleDoseMl()).isEqualByComparingTo("4.00");
        assertThat(res.frequencyPerDay()).isEqualTo(3);
    }

    @Test
    void scheduleTelemedicineSessionSuccessfully() {
        Patient patient = new Patient("MRN-001", null, "Amr Hassan", "01001", "MALE", "1990-01-01", null, null, null, null, null);
        when(patientRepository.findById("PAT-1")).thenReturn(Optional.of(patient));
        when(telemedRepository.save(any(TelemedicineSession.class))).thenAnswer(inv -> inv.getArgument(0));

        ScheduleTelemedicineSessionRequest req = new ScheduleTelemedicineSessionRequest(
                "PAT-1",
                "DOC-1",
                "Dr. Tarek Fouad",
                System.currentTimeMillis() + 3600000,
                "bemo-clinic-room-101"
        );

        TelemedicineSessionDto dto = service.scheduleTelemedicineSession(req);
        assertThat(dto.patientId()).isEqualTo("PAT-1");
        assertThat(dto.doctorName()).isEqualTo("Dr. Tarek Fouad");
        assertThat(dto.meetingLink()).contains("https://meet.jit.si/bemo-clinic-room-101");
        assertThat(dto.status()).isEqualTo("SCHEDULED");
    }

    @Test
    void registerMedicalLicenseComputesStatus() {
        when(licenseRepository.save(any(MedicalLicenseRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        long now = System.currentTimeMillis();
        long oneYearMs = 365L * 24 * 60 * 60 * 1000;

        RegisterMedicalLicenseRequest req = new RegisterMedicalLicenseRequest(
                "DOC-1",
                "Dr. Tarek Fouad",
                "PHYSICIAN",
                "MOH-88392-EG",
                "MOH",
                now,
                now + oneYearMs
        );

        MedicalLicenseRecordDto dto = service.registerLicense(req);
        assertThat(dto.licenseNumber()).isEqualTo("MOH-88392-EG");
        assertThat(dto.issuingAuthority()).isEqualTo("MOH");
        assertThat(dto.status()).isEqualTo("VALID");
    }
}
