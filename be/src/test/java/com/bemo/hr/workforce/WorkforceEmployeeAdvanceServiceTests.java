package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkforceEmployeeAdvanceServiceTests {
    @Mock private WorkforceAdvanceRepository advanceRepository;
    @Mock private WorkforceAdvanceInstallmentRepository installmentRepository;
    @Mock private WorkforceAdvanceLedgerEntryRepository ledgerRepository;
    @Mock private WorkerRepository workerRepository;
    @Mock private WorkerCategoryRepository workerCategoryRepository;
    @Mock private ContractorRepository contractorRepository;
    @Mock private AuditService auditService;
    @Mock private WorkforceAdvancePolicyRepository policyRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private AttendanceCategoryRepository attendanceCategoryRepository;
    @Mock private OperationsService operationsService;
    private WorkforceAdvanceService service;

    @BeforeEach
    void setUp() {
        service = new WorkforceAdvanceService(advanceRepository, installmentRepository, ledgerRepository,
                workerRepository, workerCategoryRepository, contractorRepository, auditService, policyRepository,
                employeeRepository, attendanceCategoryRepository, operationsService);
    }

    @Test
    void createsEmployeeAdvanceInstallmentsAndMirrorsTheEmployeeLedger() {
        Employee employee = employee("EMP-1");
        AttendanceCategory category = eligibleCategory(true);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(attendanceCategoryRepository.findById(employee.getCategoryId())).thenReturn(Optional.of(category));
        when(policyRepository.findAllByOrderByScopeTypeAscScopeIdAsc()).thenReturn(List.of());
        when(advanceRepository.save(any())).thenAnswer(invocation -> {
            WorkforceAdvance advance = invocation.getArgument(0);
            advance.prePersist();
            return advance;
        });

        WorkforceApi.AdvanceResponse response = service.create(new WorkforceApi.AdvanceCreateRequest(
                "EMPLOYEE", null, null, employee.getId(), new BigDecimal("1200"), "LONG_TERM",
                3, new BigDecimal("400"), "MONTHLY", new BigDecimal("50"), "سلفة موظف",
                "2026-08-01", "AUTO", 0), "admin");

        assertThat(response.recipientType()).isEqualTo("EMPLOYEE");
        assertThat(response.employeeId()).isEqualTo(employee.getId());
        assertThat(response.employeeName()).isEqualTo(employee.getFullName());
        assertThat(response.totalInstallments()).isEqualTo(3);
        verify(installmentRepository, org.mockito.Mockito.times(3)).save(any());
        verify(operationsService).recordAdvanceIssuance(eq(employee.getId()), eq(new BigDecimal("1200")),
                eq("ADVANCE"), any(), any(), eq("admin"));
    }

    @Test
    void rejectsEmployeeWhoseCategoryDoesNotAllowAdvances() {
        Employee employee = employee("EMP-2");
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(attendanceCategoryRepository.findById(employee.getCategoryId()))
                .thenReturn(Optional.of(eligibleCategory(false)));

        assertThatThrownBy(() -> service.create(new WorkforceApi.AdvanceCreateRequest(
                "EMPLOYEE", null, null, employee.getId(), new BigDecimal("500"), "SHORT_TERM",
                1, new BigDecimal("500"), "MONTHLY", new BigDecimal("50"), null,
                "2026-08-01", "AUTO", 0), "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("لا تسمح");
        verify(advanceRepository, never()).save(any());
    }

    @Test
    void payrollSuggestionHonorsTheDueInstallmentAndKeepsLegacyBalanceCompatible() {
        Employee employee = employee("EMP-3");
        WorkforceAdvance advance = new WorkforceAdvance("EMPLOYEE", null, null, employee.getId(),
                new BigDecimal("1000"), "LONG_TERM", 4, new BigDecimal("250"),
                "MONTHLY", new BigDecimal("50"), null, "2026-08-01", "AUTO", 0);
        WorkforceAdvanceInstallment installment = new WorkforceAdvanceInstallment(
                advance.getId(), 1, "2026-08-01", new BigDecimal("250"));
        when(advanceRepository.findByEmployeeIdOrderByCreatedAtAsc(employee.getId())).thenReturn(List.of(advance));
        when(installmentRepository.findByAdvanceId(advance.getId())).thenReturn(List.of(installment));

        BigDecimal deduction = service.calculateEmployeePayrollDeduction(
                employee.getId(), LocalDate.of(2026, 8, 31), new BigDecimal("1000"), new BigDecimal("1200"));

        assertThat(deduction).isEqualByComparingTo("450");
    }

    private Employee employee(String code) {
        return new Employee(code, "موظف " + code, null, "employee-category",
                EmploymentType.FIXED, new BigDecimal("5000"), LocalDate.of(2026, 1, 1), null, true);
    }

    private AttendanceCategory eligibleCategory(boolean eligible) {
        AttendanceCategory category = new AttendanceCategory("STAFF", "الموظفون", 480,
                PayCycle.MONTHLY, AttendanceMode.MANUAL, false, 31, true);
        category.configureAdvanceEligibility(eligible);
        return category;
    }
}
