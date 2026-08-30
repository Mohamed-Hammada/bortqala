package com.bemo.hr.medical;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.InsuranceService;
import com.bemo.hr.medical.domain.*;
import com.bemo.hr.medical.infrastructure.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsuranceServiceTests {

    @Mock
    private InsurancePayerRepository payerRepository;
    @Mock
    private InsurancePlanRepository planRepository;
    @Mock
    private PatientInsurancePolicyRepository policyRepository;
    @Mock
    private InsurancePreAuthorizationRepository preAuthRepository;
    @Mock
    private InsuranceClaimBatchRepository batchRepository;
    @Mock
    private InsuranceClaimLineRepository lineRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private ClinicVisitRepository visitRepository;

    private InsuranceService insuranceService;

    private final String APP_ID = "tenant-medical";
    private final String PATIENT_ID = "pat-1";
    private final String PAYER_ID = "payer-1";
    private final String PLAN_ID = "plan-1";
    private final String BATCH_ID = "batch-1";
    private final String LINE_ID = "line-1";

    @BeforeEach
    void setUp() {
        TenantContext.set(APP_ID);
        insuranceService = new InsuranceService(
                payerRepository,
                planRepository,
                policyRepository,
                preAuthRepository,
                batchRepository,
                lineRepository,
                patientRepository,
                visitRepository
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void calculateSplit_1000Fee_80Percent_50Copay_gives750Insurer_250Patient() {
        // AC-1 Split math
        PatientInsurancePolicy policy = new PatientInsurancePolicy(PATIENT_ID, PLAN_ID, "MEM-12345", "2026-01-01", "2026-12-31", true);
        when(policyRepository.findByAppIdAndPatientIdAndIsPrimaryTrue(APP_ID, PATIENT_ID)).thenReturn(Optional.of(policy));

        InsurancePlan plan = new InsurancePlan(PAYER_ID, "Gold 80", BigDecimal.valueOf(80), BigDecimal.valueOf(50), null, null);
        when(planRepository.findByAppIdAndId(APP_ID, PLAN_ID)).thenReturn(Optional.of(plan));

        CalculateInsuranceSplitRequest req = new CalculateInsuranceSplitRequest(PATIENT_ID, BigDecimal.valueOf(1000), "2026-08-29");
        InsuranceSplitCalculationResult result = insuranceService.calculateSplit(req);

        assertTrue(result.isPolicyValid());
        assertEquals(0, BigDecimal.valueOf(250).compareTo(result.patientShare()));
        assertEquals(0, BigDecimal.valueOf(750).compareTo(result.insurerShare()));
    }

    @Test
    void calculateSplit_expiredPolicy_fallsBackToFullPatientShare() {
        // AC-2 Expired policy blocks insurance
        PatientInsurancePolicy policy = new PatientInsurancePolicy(PATIENT_ID, PLAN_ID, "MEM-12345", "2025-01-01", "2025-12-31", true);
        when(policyRepository.findByAppIdAndPatientIdAndIsPrimaryTrue(APP_ID, PATIENT_ID)).thenReturn(Optional.of(policy));

        CalculateInsuranceSplitRequest req = new CalculateInsuranceSplitRequest(PATIENT_ID, BigDecimal.valueOf(500), "2026-08-29");
        InsuranceSplitCalculationResult result = insuranceService.calculateSplit(req);

        assertFalse(result.isPolicyValid());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(result.patientShare()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.insurerShare()));
    }

    @Test
    void preAuth_requestAndApprove() {
        // AC-3 Pre-authorization flow
        InsurancePayer payer = new InsurancePayer("AXA Egypt", InsurancePayer.Type.PRIVATE, null, null);
        when(payerRepository.findByAppIdAndId(APP_ID, PAYER_ID)).thenReturn(Optional.of(payer));

        Patient patient = new Patient("MRN-0001", "29501011234567", "Ahmed Ali", "01012345678", "MALE", "1995-01-01", "O_POS", null, null, null, null);
        when(patientRepository.findByAppIdAndId(APP_ID, PATIENT_ID)).thenReturn(Optional.of(patient));

        InsurancePreAuthorization preAuth = new InsurancePreAuthorization(PAYER_ID, PATIENT_ID, "vis-1", "MRI Brain", "AUTH-999", BigDecimal.valueOf(3500));
        preAuth.setId("auth-1");
        when(preAuthRepository.save(any(InsurancePreAuthorization.class))).thenReturn(preAuth);

        RequestPreAuthorizationRequest req = new RequestPreAuthorizationRequest(PAYER_ID, PATIENT_ID, "vis-1", "MRI Brain", "AUTH-999", BigDecimal.valueOf(3500));
        InsurancePreAuthorizationDto dto = insuranceService.requestPreAuthorization(req);
        assertEquals("REQUESTED", dto.status());

        when(preAuthRepository.findByAppIdAndId(APP_ID, "auth-1")).thenReturn(Optional.of(preAuth));
        InsurancePreAuthorizationDto decided = insuranceService.decidePreAuthorization("auth-1", new DecidePreAuthorizationRequest("APPROVED", BigDecimal.valueOf(3000)));

        assertEquals("APPROVED", decided.status());
        assertEquals(0, BigDecimal.valueOf(3000).compareTo(decided.approvedAmount()));
    }

    @Test
    void claimBatch_settleReconciliation_and_resubmit() {
        // AC-4 & AC-5: Batch reconciliation and resubmission
        InsuranceClaimBatch batch = new InsuranceClaimBatch("BATCH-2026-08", PAYER_ID, "2026-08", "Monthly claim");
        batch.setId(BATCH_ID);
        batch.submit();

        InsuranceClaimLine line1 = new InsuranceClaimLine(BATCH_ID, "v-1", PATIENT_ID, "MRN-1", "Patient A", "MEM-1", "Consultation", BigDecimal.valueOf(1000), BigDecimal.valueOf(800), BigDecimal.valueOf(200));
        line1.setId(LINE_ID);

        when(batchRepository.findByAppIdAndId(APP_ID, BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(InsuranceClaimBatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lineRepository.findAllByAppIdAndBatchIdOrderByCreatedAtAsc(APP_ID, BATCH_ID)).thenReturn(List.of(line1));

        SettleClaimBatchRequest settleReq = new SettleClaimBatchRequest(
                List.of(new SettleLineDecision(LINE_ID, "REJECTED", "Missing doctor signature")),
                "Settled with rejections"
        );

        InsuranceClaimBatchDto settled = insuranceService.settleClaimBatch(BATCH_ID, settleReq);
        assertEquals("REJECTED", settled.status());
        assertEquals(0, BigDecimal.valueOf(800).compareTo(settled.totalRejectedAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(settled.totalApprovedAmount()));

        // Resubmit into draft batch
        InsuranceClaimBatch newBatch = new InsuranceClaimBatch("BATCH-2026-09", PAYER_ID, "2026-09", "Next month");
        newBatch.setId("batch-2");
        when(batchRepository.findByAppIdAndId(APP_ID, "batch-2")).thenReturn(Optional.of(newBatch));
        when(lineRepository.findByAppIdAndId(APP_ID, LINE_ID)).thenReturn(Optional.of(line1));
        when(lineRepository.save(any(InsuranceClaimLine.class))).thenAnswer(inv -> inv.getArgument(0));

        ResubmitClaimLineRequest resubmitReq = new ResubmitClaimLineRequest(LINE_ID, "batch-2", BigDecimal.valueOf(800), "Corrected doctor signature");
        InsuranceClaimLineDto resubmitted = insuranceService.resubmitClaimLine(resubmitReq);

        assertNotNull(resubmitted);
        assertEquals(LINE_ID, resubmitted.resubmittedLineId());
        assertEquals("batch-2", resubmitted.batchId());
    }
}
