package com.bemo.hr.medical;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DentalSpecialtyServiceTests {

    private static final String APP_ID = "app-medical-test";

    @Mock
    private DentalRecordRepository dentalRecordRepository;
    @Mock
    private DentalTreatmentPlanRepository dentalTreatmentPlanRepository;
    @Mock
    private DentalTreatmentPlanItemRepository dentalTreatmentPlanItemRepository;
    @Mock
    private ExamTemplateRepository examTemplateRepository;
    @Mock
    private ExamAnswerRepository examAnswerRepository;
    @Mock
    private PatientRepository patientRepository;

    private DentalSpecialtyService dentalSpecialtyService;

    @BeforeEach
    void setUp() {
        TenantContext.set(APP_ID);
        dentalSpecialtyService = new DentalSpecialtyService(
                dentalRecordRepository,
                dentalTreatmentPlanRepository,
                dentalTreatmentPlanItemRepository,
                examTemplateRepository,
                examAnswerRepository,
                patientRepository
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void recordToothCondition_ValidTooth_Success() {
        Patient patient = new Patient("MRN-001", "29501011234567", "Amr Hassan", "01001234567", "MALE", "1995-01-01", "O_POS", null, null, null, null);
        patient.setId("pat-1");

        when(patientRepository.findByAppIdAndId(APP_ID, "pat-1")).thenReturn(Optional.of(patient));
        when(dentalRecordRepository.save(any(DentalRecord.class))).thenAnswer(i -> i.getArgument(0));

        RecordToothConditionRequest request = new RecordToothConditionRequest(
                "vis-1", 16, "CARIES", "OCCLUSAL", "Deep caries on upper first molar", System.currentTimeMillis()
        );

        DentalRecordDto result = dentalSpecialtyService.recordToothCondition("pat-1", request);

        assertThat(result).isNotNull();
        assertThat(result.toothNumber()).isEqualTo(16);
        assertThat(result.condition()).isEqualTo("CARIES");
        assertThat(result.surface()).isEqualTo("OCCLUSAL");
        assertThat(result.notes()).contains("Deep caries");
    }

    @Test
    void recordToothCondition_InvalidTooth_ThrowsFdiInvalid() {
        Patient patient = new Patient("MRN-001", "29501011234567", "Amr Hassan", "01001234567", "MALE", "1995-01-01", "O_POS", null, null, null, null);
        patient.setId("pat-1");

        when(patientRepository.findByAppIdAndId(APP_ID, "pat-1")).thenReturn(Optional.of(patient));

        RecordToothConditionRequest request = new RecordToothConditionRequest(
                "vis-1", 99, "CARIES", "OCCLUSAL", "Invalid tooth", System.currentTimeMillis()
        );

        assertThatThrownBy(() -> dentalSpecialtyService.recordToothCondition("pat-1", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid FDI tooth number")
                .matches(e -> "DENTAL_TOOTH_INVALID".equals(((BusinessRuleException) e).getCode()));
    }

    @Test
    void getPatientOdontogram_AggregatesLatestConditionPerTooth() {
        Patient patient = new Patient("MRN-001", "29501011234567", "Amr Hassan", "01001234567", "MALE", "1995-01-01", "O_POS", null, null, null, null);
        patient.setId("pat-1");

        DentalRecord rec1 = new DentalRecord("pat-1", "vis-1", 21, DentalRecord.Condition.FILLED, DentalRecord.Surface.MESIAL, "Composite fill", 2000L);
        DentalRecord rec2 = new DentalRecord("pat-1", "vis-1", 21, DentalRecord.Condition.CARIES, DentalRecord.Surface.MESIAL, "Initial caries", 1000L);

        when(patientRepository.findByAppIdAndId(APP_ID, "pat-1")).thenReturn(Optional.of(patient));
        when(dentalRecordRepository.findAllByAppIdAndPatientIdOrderByNotedOnDesc(APP_ID, "pat-1"))
                .thenReturn(List.of(rec1, rec2));

        PatientOdontogramDto odontogram = dentalSpecialtyService.getPatientOdontogram("pat-1");

        assertThat(odontogram).isNotNull();
        assertThat(odontogram.teeth()).hasSize(32);

        var tooth21 = odontogram.teeth().stream().filter(t -> t.toothNumber() == 21).findFirst().orElseThrow();
        assertThat(tooth21.condition()).isEqualTo("FILLED");
        assertThat(tooth21.surface()).isEqualTo("MESIAL");

        var tooth11 = odontogram.teeth().stream().filter(t -> t.toothNumber() == 11).findFirst().orElseThrow();
        assertThat(tooth11.condition()).isEqualTo("HEALTHY");

        assertThat(odontogram.history()).hasSize(2);
    }

    @Test
    void recordToothCondition_InvalidEnums_FallBackToDefaults() {
        Patient patient = new Patient("MRN-001", "29501011234567", "Amr Hassan", "01001234567", "MALE", "1995-01-01", "O_POS", null, null, null, null);
        patient.setId("pat-1");

        when(patientRepository.findByAppIdAndId(APP_ID, "pat-1")).thenReturn(Optional.of(patient));
        when(dentalRecordRepository.save(any(DentalRecord.class))).thenAnswer(i -> i.getArgument(0));

        RecordToothConditionRequest request = new RecordToothConditionRequest(
                "vis-1", 16, "NOT_A_CONDITION", "NOT_A_SURFACE", "fallback", System.currentTimeMillis()
        );

        DentalRecordDto result = dentalSpecialtyService.recordToothCondition("pat-1", request);

        assertThat(result).isNotNull();
        assertThat(result.condition()).isEqualTo("CARIES");
        assertThat(result.surface()).isNull();
    }

    @Test
    void addPlanItemAndMarkDone_SuccessAndIdempotent() {
        DentalTreatmentPlan plan = new DentalTreatmentPlan("pat-1", "Comprehensive Dental Rehab");
        plan.setId("plan-1");

        DentalTreatmentPlanItem item = new DentalTreatmentPlanItem("plan-1", 16, "Composite Restoration", new BigDecimal("450.00"));
        item.setId("item-1");

        when(dentalTreatmentPlanRepository.findByAppIdAndId(APP_ID, "plan-1")).thenReturn(Optional.of(plan));
        when(dentalTreatmentPlanItemRepository.save(any(DentalTreatmentPlanItem.class))).thenAnswer(i -> i.getArgument(0));
        when(dentalTreatmentPlanItemRepository.findByAppIdAndId(APP_ID, "item-1")).thenReturn(Optional.of(item));

        AddDentalPlanItemRequest addReq = new AddDentalPlanItemRequest(16, "Composite Restoration", new BigDecimal("450.00"));
        DentalTreatmentPlanItemDto added = dentalSpecialtyService.addPlanItem("plan-1", addReq);

        assertThat(added.status()).isEqualTo("PLANNED");
        assertThat(added.toothNumber()).isEqualTo(16);

        DentalTreatmentPlanItemDto done1 = dentalSpecialtyService.markPlanItemDone("item-1", "vis-10");
        assertThat(done1.status()).isEqualTo("DONE");
        assertThat(done1.completedAt()).isNotNull();

        DentalTreatmentPlanItemDto done2 = dentalSpecialtyService.markPlanItemDone("item-1", "vis-10");
        assertThat(done2.status()).isEqualTo("DONE");
    }

    @Test
    void saveAndSubmitExamTemplate_Success() {
        ExamTemplate template = new ExamTemplate("OPHTHALMOLOGY", "Fundus & Slit Lamp Exam", "{\"fields\":[{\"key\":\"intraocularPressure\",\"type\":\"number\"}]}");
        template.setId("tmpl-1");

        when(examTemplateRepository.save(any(ExamTemplate.class))).thenAnswer(i -> i.getArgument(0));
        when(examTemplateRepository.findByAppIdAndId(APP_ID, "tmpl-1")).thenReturn(Optional.of(template));
        when(examAnswerRepository.save(any(ExamAnswer.class))).thenAnswer(i -> i.getArgument(0));
        when(examAnswerRepository.findAllByAppIdAndVisitIdOrderByRecordedAtDesc(APP_ID, "vis-1"))
                .thenReturn(List.of(new ExamAnswer("vis-1", "tmpl-1", "{\"intraocularPressure\":16}", "Dr. Tarek")));

        SaveExamTemplateRequest saveReq = new SaveExamTemplateRequest("OPHTHALMOLOGY", "Fundus & Slit Lamp Exam", "{\"fields\":[{\"key\":\"intraocularPressure\",\"type\":\"number\"}]}");
        ExamTemplateDto savedTmpl = dentalSpecialtyService.saveExamTemplate(saveReq);
        assertThat(savedTmpl.specialty()).isEqualTo("OPHTHALMOLOGY");

        SubmitExamAnswerRequest answerReq = new SubmitExamAnswerRequest("vis-1", "tmpl-1", "{\"intraocularPressure\":16}", "Dr. Tarek");
        ExamAnswerDto answerDto = dentalSpecialtyService.submitExamAnswers(answerReq);
        assertThat(answerDto.answersJson()).contains("16");

        List<ExamAnswerDto> answers = dentalSpecialtyService.getExamAnswers("vis-1");
        assertThat(answers).hasSize(1);
    }
}
