package com.bemo.hr.medical;

import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.medical.api.MedicalClinicApi.DoctorCommissionStatementResponse;
import com.bemo.hr.medical.application.ClinicCommissionService;
import com.bemo.hr.medical.application.ClinicPrescriptionService;
import com.bemo.hr.medical.domain.ClinicVisit;
import com.bemo.hr.medical.infrastructure.ClinicVisitRepository;
import com.bemo.hr.medical.infrastructure.PatientRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClinicCommissionServiceTests {

    private ClinicVisitRepository visitRepository;
    private PatientRepository patientRepository;
    private EmployeeRepository employeeRepository;
    private ClinicPrescriptionService prescriptionService;
    private ClinicCommissionService commissionService;

    @BeforeEach
    void setUp() {
        TenantContext.set("app-clinic-1");
        visitRepository = mock(ClinicVisitRepository.class);
        patientRepository = mock(PatientRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        prescriptionService = mock(ClinicPrescriptionService.class);
        commissionService = new ClinicCommissionService(visitRepository, patientRepository, employeeRepository, prescriptionService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Calculate 20 completed visits fixture at 60% doctor commission")
    void test20VisitsCommissionCalculation() {
        List<ClinicVisit> visits = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            ClinicVisit v = new ClinicVisit("pat-" + i, "doc-1", "2026-08-" + String.format("%02d", (i % 28) + 1), i, new BigDecimal("250.00"), BigDecimal.ZERO, "CASH");
            v.setStatus(ClinicVisit.Status.DONE);
            visits.add(v);
        }

        when(visitRepository.findCompletedVisitsForDoctorInPeriod("app-clinic-1", "doc-1", "2026-08")).thenReturn(visits);
        Employee doctor = mock(Employee.class);
        when(doctor.getFullName()).thenReturn("Dr. Mostafa Kamel");
        when(employeeRepository.findById("doc-1")).thenReturn(Optional.of(doctor));

        // 20 visits * 250 EGP = 5,000 EGP total revenue. At 60% commission rate = 3,000.00 EGP
        DoctorCommissionStatementResponse statement = commissionService.getMonthlyStatement("doc-1", 2026, 8, new BigDecimal("60.00"));

        assertNotNull(statement);
        assertEquals("doc-1", statement.doctorEmployeeId());
        assertEquals("Dr. Mostafa Kamel", statement.doctorName());
        assertEquals("2026-08", statement.period());
        assertEquals(20, statement.completedVisitsCount());
        assertEquals(new BigDecimal("5000.00"), statement.totalRevenue());
        assertEquals(new BigDecimal("60.00"), statement.commissionRatePercent());
        assertEquals(new BigDecimal("3000.00"), statement.commissionAmount());
        assertEquals(20, statement.visits().size());
    }
}
