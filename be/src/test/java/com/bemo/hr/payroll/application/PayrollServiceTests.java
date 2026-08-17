package com.bemo.hr.payroll.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.payroll.api.PayrollApi;
import com.bemo.hr.payroll.domain.*;
import com.bemo.hr.payroll.infrastructure.PayrollRunHeaderRepository;
import com.bemo.hr.payroll.infrastructure.PayrollRunLineRepository;
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
import com.bemo.hr.reporting.application.AttendanceExceptionService;
import com.bemo.hr.reporting.domain.AttendanceReport;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.workforce.WorkforceAdvanceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTests {

    @Mock
    private SalaryPaymentRepository salaryPaymentRepository;
    @Mock
    private SalaryPaymentExplanationRepository explanationRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private AttendanceCategoryRepository attendanceCategoryRepository;
    @Mock
    private AttendanceReportRepository attendanceReportRepository;
    @Mock
    private DailyAttendanceResultRepository dailyAttendanceResultRepository;
    @Mock
    private OperationsService operationsService;
    @Mock
    private WorkforceAdvanceService workforceAdvanceService;
    @Mock
    private PayrollExcelExporter payrollExcelExporter;
    @Mock
    private AuditService auditService;
    @Mock
    private AttendanceExceptionService attendanceExceptionService;
    @Mock
    private PayrollSnapshotService payrollSnapshotService;
    @Mock
    private PayrollCalculationPolicyService payrollCalculationPolicyService;
    @Mock
    private PayrollRunHeaderRepository payrollRunHeaderRepository;
    @Mock
    private PayrollRunLineRepository payrollRunLineRepository;
    @Mock
    private PayrollGlPostingService payrollGlPostingService;

    @InjectMocks
    private PayrollService payrollService;

    @BeforeEach
    void setUp() {
        TenantContext.set("test-tenant");
        lenient().when(payrollCalculationPolicyService.effectivePolicy(any())).thenReturn(
                new PayrollCalculationPolicy("Test", LocalDate.of(2000, 1, 1), null,
                        new BigDecimal("240"), new BigDecimal("1.5")));
        lenient().when(payrollSnapshotService.find(anyString(), anyString())).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getSheet_returnsEmptySheet_whenNoEmployees() {
        when(employeeRepository.findAllByOrderByFullNameAsc()).thenReturn(Collections.emptyList());
        var response = payrollService.getSheet(2026, 8, null);
        assertThat(response.rows()).isEmpty();
    }


    @Test
    void reversePayment_throwsWhenPaymentNotFound() {
        var req = new PayrollApi.ReversePaymentRequest("missing-id", "reason", 0L);


        when(salaryPaymentRepository.findByIdForUpdate("missing-id")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> payrollService.reversePayment(req, "admin"))
                .isInstanceOf(com.bemo.hr.shared.domain.NotFoundException.class)
                .hasMessageContaining("قيد الراتب غير موجود.");
    }

    @Test
    void recordPayment_enforcesTheAttendanceExceptionGate() {
        Employee employee = new Employee("E-1", "Employee", null, "cat-1", EmploymentType.FIXED,
                new BigDecimal("5000"), LocalDate.of(2026, 1, 1), null, true);
        AttendanceReport report = new AttendanceReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                PayCycle.MONTHLY, "cfg", "admin");
        report.startReview(0);
        report.approve("admin");
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        SalaryPayment payment = new SalaryPayment(employee.getId(), report.getId(), 2026, 8, "FULL_MONTH",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), new BigDecimal("5000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("5000"),
                com.bemo.hr.payroll.domain.PaymentStatus.DRAFT, null, null, null, null, "maker");
        payment.transitionTo(com.bemo.hr.payroll.domain.PaymentStatus.CALCULATED);
        payment.transitionTo(com.bemo.hr.payroll.domain.PaymentStatus.REVIEWED);
        payment.transitionTo(com.bemo.hr.payroll.domain.PaymentStatus.APPROVED);
        payment.transitionTo(com.bemo.hr.payroll.domain.PaymentStatus.POSTED);
        PayrollRunHeader run = new PayrollRunHeader("PAY-2026-08", "2026-08:FULL_MONTH", LocalDate.of(2026, 8, 31));
        run.updateTotals(new BigDecimal("5000"), BigDecimal.ZERO, new BigDecimal("5000"));
        run.transitionTo(PayrollRunHeader.Status.REVIEWED);
        run.transitionTo(PayrollRunHeader.Status.APPROVED);
        run.transitionTo(PayrollRunHeader.Status.POSTED);
        PayrollInputSnapshot snapshot = mock(PayrollInputSnapshot.class);
        payment.attachCalculationEvidence(run.getId(), "snapshot-1");
        when(salaryPaymentRepository.findForUpdate(employee.getId(), 2026, 8, "FULL_MONTH"))
                .thenReturn(Optional.of(payment));
        when(payrollSnapshotService.findById("snapshot-1")).thenReturn(Optional.of(snapshot));
        when(payrollRunHeaderRepository.findByIdForUpdate(run.getId())).thenReturn(Optional.of(run));
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(PayCycle.MONTHLY,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))).thenReturn(Optional.of(report));
        doThrow(new BusinessRuleException("blocked", "PAYROLL_ATTENDANCE_EXCEPTIONS_OPEN", HttpStatus.CONFLICT))
                .when(attendanceExceptionService).assertPayrollReady(report.getId(), employee.getId());
        PayrollApi.PaymentRequest request = new PayrollApi.PaymentRequest(employee.getId(), 2026, 8, "FULL_MONTH",
                null, null, null, null, 0L);
        assertThatThrownBy(() -> payrollService.recordPayment(request, "payroll"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("blocked");
        verify(attendanceExceptionService).assertPayrollReady(report.getId(), employee.getId());
        verify(salaryPaymentRepository, never()).save(any());
    }

    @Test
    void paymentExplanationUsesThePersistedPayrollSnapshot() {
        SalaryPayment payment = new SalaryPayment("emp-1", null, 2026, 8, "FULL_MONTH",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("5325.00"), BigDecimal.ZERO, new BigDecimal("50.00"),
                new BigDecimal("375.00"), new BigDecimal("5275.00"), null,
                null, null, null, null, "payroll");
        PayrollInputSnapshot snapshot = new PayrollInputSnapshot("run-1", "emp-1", "2026-08:FULL_MONTH",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), new BigDecimal("5000.00"),
                9600, 600, 60, 1, "policy-1", 3, new BigDecimal("200.00"),
                new BigDecimal("1.25"), new BigDecimal("50.00"), new BigDecimal("375.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("5375.00"), new BigDecimal("5325.00"), "payroll");
        payment.attachCalculationEvidence("run-1", snapshot.getId());
        List<SalaryPaymentExplanation> persisted = new java.util.ArrayList<>();

        when(explanationRepository.findBySalaryPaymentIdOrderByCreatedAtAsc(payment.getId()))
                .thenAnswer(invocation -> List.copyOf(persisted));
        when(salaryPaymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(payrollSnapshotService.findById(snapshot.getId())).thenReturn(Optional.of(snapshot));
        when(explanationRepository.save(any(SalaryPaymentExplanation.class))).thenAnswer(invocation -> {
            SalaryPaymentExplanation explanation = invocation.getArgument(0);
            persisted.add(explanation);
            return explanation;
        });

        var explanations = payrollService.getPaymentExplanation(payment.getId());

        assertThat(explanations).hasSize(2);
        assertThat(explanations.get(0).componentType()).isEqualTo("SNAPSHOT_CALCULATION");
        assertThat(explanations.get(0).inputValuesJson())
                .contains(snapshot.getId(), "\"baseSalary\":5000.00", "\"workingHourDivisor\":200.00",
                        "\"overtimeMultiplier\":1.25");
        assertThat(explanations.get(0).calculatedAmount()).isEqualByComparingTo(payment.getGrossAmount());
        assertThat(explanations.get(1).calculatedAmount()).isEqualByComparingTo(payment.getNetAmount());
    }

    // A golden example for 8-hour category should ideally be a fully verified test using actual logic.
    // Right now, this acts as the foundational unit test class for the service.
}
