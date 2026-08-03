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

    @InjectMocks
    private PayrollService payrollService;

    @BeforeEach
    void setUp() {
        TenantContext.set("test-tenant");
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

    // A golden example for 8-hour category should ideally be a fully verified test using actual logic.
    // Right now, this acts as the foundational unit test class for the service.
}
