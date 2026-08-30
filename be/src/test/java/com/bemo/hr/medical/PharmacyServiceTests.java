package com.bemo.hr.medical;

import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.PharmacyService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceTests {

    @Mock
    private PharmacyItemRepository itemRepository;
    @Mock
    private PharmacyDispenseRecordRepository recordRepository;
    @Mock
    private PharmacyDispenseLineRepository lineRepository;
    @Mock
    private NarcoticsRegisterRepository narcoticsRepository;
    @Mock
    private ClinicVisitRepository visitRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    private PharmacyService pharmacyService;

    private final String APP_ID = "tenant-medical";
    private final String RX_ID = "rx-1";
    private final String DRUG_ID = "drug-1";
    private final String PATIENT_ID = "pat-1";
    private final String DOCTOR_ID = "doc-1";

    @BeforeEach
    void setUp() {
        TenantContext.set(APP_ID);
        pharmacyService = new PharmacyService(
                itemRepository,
                recordRepository,
                lineRepository,
                narcoticsRepository,
                visitRepository,
                patientRepository,
                employeeRepository
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void dispensePrescription_rejectsExpiredBatch() {
        ClinicVisit visit = new ClinicVisit(PATIENT_ID, DOCTOR_ID, "2026-08-30", 1, BigDecimal.ZERO, BigDecimal.ZERO, "CASH");
        when(visitRepository.findByAppIdAndId(APP_ID, RX_ID)).thenReturn(Optional.of(visit));

        PharmacyItem drug = new PharmacyItem("item-1", "Panadol 500mg", "Paracetamol", PharmacyItem.DosageForm.TABLET, "500mg", false, null);
        when(itemRepository.findByAppIdAndId(APP_ID, DRUG_ID)).thenReturn(Optional.of(drug));

        String yesterday = LocalDate.now().minusDays(1).toString();
        DispenseLineRequest lineReq = new DispenseLineRequest("line-1", DRUG_ID, "BATCH-01", yesterday, BigDecimal.valueOf(10));
        DispensePrescriptionRequest request = new DispensePrescriptionRequest(null, null, List.of(lineReq));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                pharmacyService.dispensePrescription(RX_ID, request, "user-pharmacist")
        );
        assertEquals("PHARM_BATCH_EXPIRED", ex.getCode());
    }

    @Test
    void dispensePrescription_controlledDrug_requiresSecondSigner_orSetsPending() {
        ClinicVisit visit = new ClinicVisit(PATIENT_ID, DOCTOR_ID, "2026-08-30", 1, BigDecimal.ZERO, BigDecimal.ZERO, "CASH");
        when(visitRepository.findByAppIdAndId(APP_ID, RX_ID)).thenReturn(Optional.of(visit));

        PharmacyItem controlledDrug = new PharmacyItem("item-2", "Morphine 10mg", "Morphine", PharmacyItem.DosageForm.INJECTION, "10mg", true, PharmacyItem.ControlSchedule.SCHEDULE_II);
        when(itemRepository.findByAppIdAndId(APP_ID, DRUG_ID)).thenReturn(Optional.of(controlledDrug));
        when(recordRepository.save(any(PharmacyDispenseRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lineRepository.save(any(PharmacyDispenseLine.class))).thenAnswer(inv -> inv.getArgument(0));

        String futureDate = LocalDate.now().plusMonths(6).toString();
        DispenseLineRequest lineReq = new DispenseLineRequest("line-1", DRUG_ID, "BATCH-M01", futureDate, BigDecimal.valueOf(2));
        DispensePrescriptionRequest requestWithoutSecondSigner = new DispensePrescriptionRequest(null, "Urgent pain relief", List.of(lineReq));

        PharmacyDispenseRecordDto result = pharmacyService.dispensePrescription(RX_ID, requestWithoutSecondSigner, "user-pharmacist-1");

        assertNotNull(result);
        assertEquals("PENDING_APPROVAL", result.status());
        assertTrue(result.controlled());
        verify(narcoticsRepository, never()).save(any());
    }

    @Test
    void approveControlledDispense_dualSignOff_writesNarcoticsRegister() {
        PharmacyDispenseRecord pending = new PharmacyDispenseRecord(
                RX_ID, PATIENT_ID, DOCTOR_ID, "user-pharmacist-1", null,
                PharmacyDispenseRecord.Status.PENDING_APPROVAL, true, "Urgent"
        );
        pending.setId("disp-1");

        when(recordRepository.findByAppIdAndId(APP_ID, "disp-1")).thenReturn(Optional.of(pending));
        when(recordRepository.save(any(PharmacyDispenseRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        PharmacyItem controlledDrug = new PharmacyItem("item-2", "Morphine 10mg", "Morphine", PharmacyItem.DosageForm.INJECTION, "10mg", true, PharmacyItem.ControlSchedule.SCHEDULE_II);
        controlledDrug.setId(DRUG_ID);

        PharmacyDispenseLine line = new PharmacyDispenseLine("disp-1", "line-1", DRUG_ID, "item-2", "BATCH-M01", "2027-01-01", BigDecimal.valueOf(2));
        when(lineRepository.findAllByAppIdAndDispenseRecordId(APP_ID, "disp-1")).thenReturn(List.of(line));
        when(itemRepository.findByAppIdAndId(APP_ID, DRUG_ID)).thenReturn(Optional.of(controlledDrug));

        PharmacyDispenseRecordDto approved = pharmacyService.approveControlledDispense("disp-1", "user-pharmacist-2");

        assertNotNull(approved);
        assertEquals("DISPENSED", approved.status());
        assertEquals("user-pharmacist-2", approved.secondSignerId());
        verify(narcoticsRepository).save(any(NarcoticsRegisterEntry.class));
    }
}
