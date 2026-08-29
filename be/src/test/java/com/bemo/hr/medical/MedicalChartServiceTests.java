package com.bemo.hr.medical;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.ClinicQueueService;
import com.bemo.hr.medical.application.MedicalChartService;
import com.bemo.hr.medical.application.PatientService;
import com.bemo.hr.medical.domain.*;
import com.bemo.hr.medical.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalChartServiceTests {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private PatientAllergyRepository allergyRepository;
    @Mock
    private PatientConditionRepository conditionRepository;
    @Mock
    private VisitVitalsRepository vitalsRepository;
    @Mock
    private PatientDocumentRepository documentRepository;
    @Mock
    private ConsentFormRepository consentRepository;
    @Mock
    private ClinicVisitRepository visitRepository;
    @Mock
    private ClinicQueueService queueService;
    @Mock
    private PatientService patientService;

    private MedicalChartService chartService;

    private final String APP_ID = "tenant-medical";
    private final String PATIENT_ID = "patient-1";
    private final String VISIT_ID = "visit-1";

    @BeforeEach
    void setUp() {
        TenantContext.set(APP_ID);
        chartService = new MedicalChartService(
                patientRepository,
                allergyRepository,
                conditionRepository,
                vitalsRepository,
                documentRepository,
                consentRepository,
                visitRepository,
                queueService,
                patientService
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getPatientChart_returnsAggregatedPayload_withSevereAllergyFlag() {
        Patient patient = new Patient("MRN-00001", "29008200101534", "Ahmed Ali", "01001234567", "MALE", "1990-08-20", "O_POSITIVE", null, null, null, null);
        patient.setId(PATIENT_ID);

        PatientAllergy severeAllergy = new PatientAllergy(PATIENT_ID, "Penicillin", PatientAllergy.Severity.SEVERE, "Anaphylaxis");
        PatientCondition chronicCondition = new PatientCondition(PATIENT_ID, "I10", "Essential Hypertension", true, "2020-01-01", PatientCondition.Status.ACTIVE, "Controlled");

        when(patientRepository.findByAppIdAndId(APP_ID, PATIENT_ID)).thenReturn(Optional.of(patient));
        when(allergyRepository.findByAppIdAndPatientIdOrderByNotedAtDesc(APP_ID, PATIENT_ID)).thenReturn(List.of(severeAllergy));
        when(conditionRepository.findByAppIdAndPatientIdOrderByCreatedAtDesc(APP_ID, PATIENT_ID)).thenReturn(List.of(chronicCondition));
        when(vitalsRepository.findByAppIdAndPatientIdOrderByRecordedAtDesc(APP_ID, PATIENT_ID)).thenReturn(List.of());
        when(documentRepository.findByAppIdAndPatientIdOrderByUploadedAtDesc(APP_ID, PATIENT_ID)).thenReturn(List.of());
        when(consentRepository.findByAppIdAndPatientIdOrderBySignedAtDesc(APP_ID, PATIENT_ID)).thenReturn(List.of());
        when(visitRepository.findAllByAppIdAndPatientIdOrderByCreatedAtDesc(APP_ID, PATIENT_ID)).thenReturn(List.of());

        PatientResponse patientResponse = new PatientResponse(PATIENT_ID, "MRN-00001", "29008200101534", "Ahmed Ali", "01001234567", "MALE", "1990-08-20", "O_POSITIVE", null, null, null, null, 1000L, 1000L);
        when(patientService.toResponse(patient)).thenReturn(patientResponse);

        PatientChartResponse chart = chartService.getPatientChart(PATIENT_ID);

        assertNotNull(chart);
        assertEquals("MRN-00001", chart.patient().mrn());
        assertTrue(chart.hasSevereAllergies());
        assertEquals(1, chart.allergies().size());
        assertEquals("Penicillin", chart.allergies().get(0).substance());
        assertEquals(1, chart.conditions().size());
        assertEquals("Essential Hypertension", chart.conditions().get(0).label());
        assertTrue(chart.conditions().get(0).chronic());
    }

    @Test
    void recordVitals_computesBmiCorrectly_andSaves() {
        ClinicVisit visit = new ClinicVisit(PATIENT_ID, "doc-1", "2026-08-29", 1, new BigDecimal("200.00"), BigDecimal.ZERO, "CASH");
        visit.setId(VISIT_ID);

        when(visitRepository.findByAppIdAndId(APP_ID, VISIT_ID)).thenReturn(Optional.of(visit));
        when(vitalsRepository.findByAppIdAndVisitId(APP_ID, VISIT_ID)).thenReturn(Optional.empty());

        when(vitalsRepository.save(any(VisitVitals.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Weight = 80 kg, Height = 175 cm -> BMI = 80 / (1.75^2) = 80 / 3.0625 = 26.1
        RecordVitalsRequest request = new RecordVitalsRequest(
                120,
                80,
                72,
                new BigDecimal("37.0"),
                98,
                new BigDecimal("80.00"),
                new BigDecimal("175.0"),
                "Normal baseline"
        );

        VisitVitalsDto saved = chartService.recordVitals(VISIT_ID, request);

        assertNotNull(saved);
        assertEquals(120, saved.systolicBp());
        assertEquals(80, saved.diastolicBp());
        assertEquals(72, saved.pulse());
        assertEquals(new BigDecimal("26.1"), saved.bmi());
    }

    @Test
    void recordVitals_rejectsInvalidPhysiologicalRanges() {
        ClinicVisit visit = new ClinicVisit(PATIENT_ID, "doc-1", "2026-08-29", 1, new BigDecimal("200.00"), BigDecimal.ZERO, "CASH");
        visit.setId(VISIT_ID);
        when(visitRepository.findByAppIdAndId(APP_ID, VISIT_ID)).thenReturn(Optional.of(visit));

        // Systolic 300 mmHg is outside valid 60-260 range
        RecordVitalsRequest invalidSystolic = new RecordVitalsRequest(
                300, 80, 72, new BigDecimal("37.0"), 98, new BigDecimal("70.00"), new BigDecimal("170.0"), null
        );

        BusinessRuleException ex1 = assertThrows(BusinessRuleException.class, () ->
                chartService.recordVitals(VISIT_ID, invalidSystolic)
        );
        assertEquals("VITALS_RANGE_INVALID", ex1.getCode());

        // Temp 45.0 °C is outside valid 34.0-43.0 range
        RecordVitalsRequest invalidTemp = new RecordVitalsRequest(
                120, 80, 72, new BigDecimal("45.0"), 98, new BigDecimal("70.00"), new BigDecimal("170.0"), null
        );
        BusinessRuleException ex2 = assertThrows(BusinessRuleException.class, () ->
                chartService.recordVitals(VISIT_ID, invalidTemp)
        );
        assertEquals("VITALS_RANGE_INVALID", ex2.getCode());
    }

    @Test
    void signConsent_savesConsentWithSignatureAndTimestamp() {
        Patient patient = new Patient("MRN-00001", "29008200101534", "Ahmed Ali", "01001234567", "MALE", "1990-08-20", "O_POSITIVE", null, null, null, null);
        when(patientRepository.findByAppIdAndId(APP_ID, PATIENT_ID)).thenReturn(Optional.of(patient));
        when(consentRepository.save(any(ConsentForm.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SignConsentRequest request = new SignConsentRequest(
                VISIT_ID,
                "PROCEDURE_CONSENT",
                "Consent for Minor Skin Biopsy",
                "I agree to the procedure as explained by the physician.",
                "Ahmed Ali",
                "SELF",
                "192.168.1.100"
        );

        ConsentFormDto consent = chartService.signConsent(PATIENT_ID, request);

        assertNotNull(consent);
        assertEquals("PROCEDURE_CONSENT", consent.templateKey());
        assertEquals("Ahmed Ali", consent.signedByName());
        assertEquals("SELF", consent.signedByRelation());
        assertTrue(consent.signedAt() > 0);
    }
}
