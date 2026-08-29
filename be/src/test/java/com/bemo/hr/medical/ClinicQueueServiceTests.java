package com.bemo.hr.medical;

import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.medical.api.MedicalClinicApi.ClinicVisitResponse;
import com.bemo.hr.medical.api.MedicalClinicApi.CompleteVisitRequest;
import com.bemo.hr.medical.api.MedicalClinicApi.PrescriptionLineRequest;
import com.bemo.hr.medical.api.MedicalClinicApi.QueueVisitRequest;
import com.bemo.hr.medical.application.ClinicPrescriptionService;
import com.bemo.hr.medical.application.ClinicQueueService;
import com.bemo.hr.medical.domain.ClinicVisit;
import com.bemo.hr.medical.domain.Patient;
import com.bemo.hr.medical.infrastructure.ClinicVisitRepository;
import com.bemo.hr.medical.infrastructure.PatientRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClinicQueueServiceTests {

    private ClinicVisitRepository visitRepository;
    private PatientRepository patientRepository;
    private EmployeeRepository employeeRepository;
    private ClinicPrescriptionService prescriptionService;
    private ClinicQueueService queueService;

    @BeforeEach
    void setUp() {
        TenantContext.set("app-clinic-1");
        visitRepository = mock(ClinicVisitRepository.class);
        patientRepository = mock(PatientRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        prescriptionService = mock(ClinicPrescriptionService.class);
        queueService = new ClinicQueueService(visitRepository, patientRepository, employeeRepository, prescriptionService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Queue visit assigns next sequential token for doctor and date")
    void testQueueVisitTokenSequence() {
        Patient patient = new Patient("MRN-00001", null, "Ali Hassan", "01000000001", "MALE", "1995-01-01", null, null, null, null, null);
        when(patientRepository.findByAppIdAndId("app-clinic-1", "pat-1")).thenReturn(Optional.of(patient));
        when(visitRepository.findMaxTokenForDoctorAndDate("app-clinic-1", "doc-1", "2026-08-29")).thenReturn(4);
        when(visitRepository.save(any(ClinicVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        QueueVisitRequest request = new QueueVisitRequest(
                "pat-1",
                "doc-1",
                "2026-08-29",
                new BigDecimal("300.00"),
                new BigDecimal("50.00"),
                "CASH"
        );

        ClinicVisitResponse response = queueService.queueVisit(request);

        assertNotNull(response);
        assertEquals(5, response.token());
        assertEquals("WAITING", response.status());
        assertEquals(new BigDecimal("300.00"), response.feeCharged());
        assertEquals(new BigDecimal("50.00"), response.insuranceCovered());
        assertEquals(new BigDecimal("250.00"), response.patientShare());

        ArgumentCaptor<ClinicVisit> captor = ArgumentCaptor.forClass(ClinicVisit.class);
        verify(visitRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getToken());
    }

    @Test
    @DisplayName("State transitions: WAITING -> call -> IN_ROOM -> complete -> DONE with prescription")
    void testStateTransitions() {
        ClinicVisit visit = new ClinicVisit("pat-1", "doc-1", "2026-08-29", 1, new BigDecimal("200.00"), BigDecimal.ZERO, "CASH");
        when(visitRepository.findByAppIdAndId("app-clinic-1", visit.getId())).thenReturn(Optional.of(visit));
        when(visitRepository.save(any(ClinicVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Call to room
        ClinicVisitResponse called = queueService.callVisit(visit.getId());
        assertEquals("IN_ROOM", called.status());

        // 2. Complete with prescription
        CompleteVisitRequest completeReq = new CompleteVisitRequest(
                "Headache and fever",
                "R51",
                "Prescribed Panadol and rest",
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                "CASH",
                List.of(new PrescriptionLineRequest("Panadol 500mg", "1 tab", "3 times daily", "5 days", "After meals"))
        );

        ClinicVisitResponse completed = queueService.completeVisit(visit.getId(), completeReq);
        assertEquals("DONE", completed.status());
        assertEquals("Headache and fever", completed.chiefComplaint());
        assertEquals("R51", completed.diagnosisIcd());
        verify(prescriptionService).savePrescriptions(eq(visit.getId()), any());
    }
}
