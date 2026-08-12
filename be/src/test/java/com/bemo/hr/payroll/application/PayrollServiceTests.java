package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.api.PayrollApi;
import com.bemo.hr.payroll.domain.SalaryPayment;
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.workforce.WorkforceAdvanceService;
import com.bemo.hr.payroll.application.PayrollExcelExporter;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.reporting.application.AttendanceExceptionService;
import com.bemo.hr.reporting.domain.AttendanceReport;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.payroll.domain.PayrollCalculationPolicy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
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

    @Mock private SalaryPaymentRepository salaryPaymentRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private AttendanceCategoryRepository attendanceCategoryRepository;
    @Mock private AttendanceReportRepository attendanceReportRepository;
    @Mock private DailyAttendanceResultRepository dailyAttendanceResultRepository;
    @Mock private OperationsService operationsService;
    @Mock private WorkforceAdvanceService workforceAdvanceService;
    @Mock private PayrollExcelExporter payrollExcelExporter;
    @Mock private AuditService auditService;
    @Mock private AttendanceExceptionService attendanceExceptionService;
    @Mock private PayrollSnapshotService payrollSnapshotService;
    @Mock private PayrollCalculationPolicyService payrollCalculationPolicyService;

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
        var req = new PayrollApi.ReversePaymentRequest("missing-id", "reason");
        
        
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
        report.startReview(0); report.approve("admin");
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(salaryPaymentRepository.findByEmployeeIdAndPeriodYearAndPeriodMonthAndPeriodKind(employee.getId(), 2026, 8, "FULL_MONTH"))
                .thenReturn(Optional.empty());
        when(attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(PayCycle.MONTHLY,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))).thenReturn(Optional.of(report));
        doThrow(new BusinessRuleException("blocked", "PAYROLL_ATTENDANCE_EXCEPTIONS_OPEN", HttpStatus.CONFLICT))
                .when(attendanceExceptionService).assertPayrollReady(report.getId(), employee.getId());
        PayrollApi.PaymentRequest request = new PayrollApi.PaymentRequest(employee.getId(), 2026, 8, "FULL_MONTH",
                null, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null, null);
        assertThatThrownBy(() -> payrollService.recordPayment(request, "payroll"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("blocked");
        verify(attendanceExceptionService).assertPayrollReady(report.getId(), employee.getId());
        verify(salaryPaymentRepository, never()).save(any());
    }

    // A golden example for 8-hour category should ideally be a fully verified test using actual logic.
    // Right now, this acts as the foundational unit test class for the service.
}
