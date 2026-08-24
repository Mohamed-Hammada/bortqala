package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.employee.domain.*;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkforceEmployeeAdvanceServiceTests {
    @Mock
    private WorkforceAdvanceRepository advanceRepository;
    @Mock
    private WorkforceAdvanceInstallmentRepository installmentRepository;
    @Mock
    private WorkforceAdvanceLedgerEntryRepository ledgerRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private ContractorRepository contractorRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private WorkforceAdvancePolicyRepository policyRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private AttendanceCategoryRepository attendanceCategoryRepository;
    @Mock
    private OperationsService operationsService;
    private WorkforceAdvanceService service;

    @BeforeEach
    void setUp() {
        service = new WorkforceAdvanceService(advanceRepository, installmentRepository, ledgerRepository,
                workerRepository, contractorRepository, auditService, policyRepository,
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
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("ADVANCE_CATEGORY_NOT_ALLOWED"));
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

    @Test
    void resolvedPolicyFallsBackToDefaultsWhenNoPoliciesExist() {
        when(policyRepository.findAllByOrderByScopeTypeAscScopeIdAsc()).thenReturn(List.of());

        WorkforceApi.ResolvedDeductionPolicyResponse resolved =
                service.resolveDeductionPolicy("emp-any", null, LocalDate.now());

        assertThat(resolved.mode()).isEqualTo("AUTO");
        assertThat(resolved.cadence()).isEqualTo("MONTHLY");
        assertThat(resolved.source()).isEqualTo("DEFAULTS");
        assertThat(resolved.manual()).isFalse();
    }

    @Test
    void categoryManualPolicyOverridesGlobalAutoAndGatesPayrollDeduction() {
        Employee employee = employee("EMP-M");
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        WorkforceAdvancePolicy categoryManual = policy("EMPLOYEE_CATEGORY", "employee-category",
                "MANUAL_BUTTON", "MID_MONTH_SPLIT", 2);
        WorkforceAdvancePolicy globalAuto = policy("GLOBAL", null, "AUTO", "MONTHLY", 1);
        when(policyRepository.findAllByOrderByScopeTypeAscScopeIdAsc())
                .thenReturn(List.of(globalAuto, categoryManual));

        WorkforceApi.ResolvedDeductionPolicyResponse resolved =
                service.resolveDeductionPolicy(employee.getId(), null, LocalDate.now());

        assertThat(resolved.mode()).isEqualTo("MANUAL");
        assertThat(resolved.cadence()).isEqualTo("MID_MONTH_SPLIT");
        assertThat(resolved.source()).isEqualTo("CATEGORY");
        assertThat(resolved.manual()).isTrue();
        assertThat(service.isManualDeductionPolicy(employee.getId(), LocalDate.now())).isTrue();
    }

    @Test
    void globalPolicyWinsWhenCategoryHasNoOverride() {
        Employee employee = employee("EMP-G");
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        WorkforceAdvancePolicy globalManual = policy("GLOBAL", null, "MANUAL", "HALF_MONTH", 1);
        when(policyRepository.findAllByOrderByScopeTypeAscScopeIdAsc()).thenReturn(List.of(globalManual));

        WorkforceApi.ResolvedDeductionPolicyResponse resolved =
                service.resolveDeductionPolicy(employee.getId(), null, LocalDate.now());

        assertThat(resolved.manual()).isTrue();
        assertThat(resolved.source()).isEqualTo("GLOBAL");
        assertThat(resolved.cadence()).isEqualTo("MID_MONTH_SPLIT");
    }

    @Test
    void manualApplyIsRejectedWhenResolvedPolicyIsAuto() {
        Employee employee = employee("EMP-A");
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(policyRepository.findAllByOrderByScopeTypeAscScopeIdAsc()).thenReturn(List.of());

        assertThatThrownBy(() -> service.applyManualDeduction(
                new WorkforceApi.ManualDeductionRequest(employee.getId(), "2026/8"), "finance"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo("ADVANCE_MANUAL_NOT_DUE"));
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void manualApplyRejectsBlankPeriodReference() {
        Employee employee = employee("EMP-P");
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.applyManualDeduction(
                new WorkforceApi.ManualDeductionRequest(employee.getId(), " "), "finance"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo("ADVANCE_POLICY_INVALID"));
    }

    @Test
    void manualApplyCollectsOnlyOverdueInstallmentsAndReplaysIdempotentlyPerPeriod() {
        Employee employee = employee("EMP-B");
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(policyRepository.findAllByOrderByScopeTypeAscScopeIdAsc())
                .thenReturn(List.of(policy("EMPLOYEE_CATEGORY", "employee-category", "MANUAL_BUTTON", "MONTHLY", 1)));
        WorkforceAdvance advance = new WorkforceAdvance("EMPLOYEE", null, null, employee.getId(),
                new BigDecimal("1000"), "LONG_TERM", 4, new BigDecimal("250"),
                "MONTHLY", new BigDecimal("50"), null, "2026-08-01", "AUTO", 0);
        advance.prePersist();
        when(advanceRepository.findByEmployeeIdOrderByCreatedAtAsc(employee.getId())).thenReturn(List.of(advance));
        WorkforceAdvanceInstallment overdue = new WorkforceAdvanceInstallment(
                advance.getId(), 1, "2026-01-01", new BigDecimal("250"));
        WorkforceAdvanceInstallment future = new WorkforceAdvanceInstallment(
                advance.getId(), 2, "2099-01-01", new BigDecimal("250"));
        when(installmentRepository.findByAdvanceId(advance.getId())).thenReturn(List.of(overdue, future));
        when(ledgerRepository.findByAdvanceId(advance.getId())).thenReturn(List.of());

        WorkforceApi.ManualDeductionResult result = service.applyManualDeduction(
                new WorkforceApi.ManualDeductionRequest(employee.getId(), "2026/8"), "finance");

        assertThat(result.duplicate()).isFalse();
        assertThat(result.appliedAmount()).isEqualByComparingTo("250");
        assertThat(result.lines()).hasSize(1);
        assertThat(result.lines().get(0).advanceId()).isEqualTo(advance.getId());
        verify(auditService).record(eq("APPLY_MANUAL_DEDUCTION"), eq("ADVANCE"), eq(employee.getId()),
                eq("finance"), any(), any());

        when(ledgerRepository.findByAdvanceId(advance.getId())).thenReturn(List.of(
                new WorkforceAdvanceLedgerEntry(advance.getId(), "PAYROLL_DEDUCTION",
                        new BigDecimal("250"), new BigDecimal("750"), "Salary deduction 2026/8", "finance")));
        WorkforceApi.ManualDeductionResult replay = service.applyManualDeduction(
                new WorkforceApi.ManualDeductionRequest(employee.getId(), "2026/8"), "finance");

        assertThat(replay.duplicate()).isTrue();
        assertThat(replay.appliedAmount()).isEqualByComparingTo("0");
    }

    @Test
    void manualApplyWithNothingDueThrowsAdvanceNothingDue() {
        Employee employee = employee("EMP-N");
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(policyRepository.findAllByOrderByScopeTypeAscScopeIdAsc())
                .thenReturn(List.of(policy("EMPLOYEE_CATEGORY", "employee-category", "MANUAL_BUTTON", "MONTHLY", 1)));
        WorkforceAdvance advance = new WorkforceAdvance("EMPLOYEE", null, null, employee.getId(),
                new BigDecimal("1000"), "LONG_TERM", 4, new BigDecimal("250"),
                "MONTHLY", new BigDecimal("50"), null, "2026-08-01", "AUTO", 0);
        advance.prePersist();
        when(advanceRepository.findByEmployeeIdOrderByCreatedAtAsc(employee.getId())).thenReturn(List.of(advance));
        WorkforceAdvanceInstallment future = new WorkforceAdvanceInstallment(
                advance.getId(), 1, "2099-01-01", new BigDecimal("250"));
        when(installmentRepository.findByAdvanceId(advance.getId())).thenReturn(List.of(future));
        when(ledgerRepository.findByAdvanceId(advance.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.applyManualDeduction(
                new WorkforceApi.ManualDeductionRequest(employee.getId(), "2026/8"), "finance"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo("ADVANCE_NOTHING_DUE"));
    }

    @Test
    void savePolicyRejectsUnknownDeductionModeWithAdvancePolicyInvalid() {
        assertThatThrownBy(() -> service.savePolicy(new WorkforceApi.AdvancePolicyRequest(
                null, "GLOBAL", null, "SOMETING_ELSE", "MONTHLY", new BigDecimal("50"),
                1, 0, "2026-08-01", "", true), "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo("ADVANCE_POLICY_INVALID"));
    }

    @Test
    void savePolicyRejectsOverlappingLaterOpenVersionWithAdvancePolicyExists() {
        WorkforceAdvancePolicy laterOpen = new WorkforceAdvancePolicy("GLOBAL", null, "AUTO", "MONTHLY",
                new BigDecimal("50"), 1, 0, true, 1, LocalDate.of(2026, 12, 1), null);
        when(policyRepository.findAllByOrderByScopeTypeAscScopeIdAsc()).thenReturn(List.of(laterOpen));

        assertThatThrownBy(() -> service.savePolicy(new WorkforceApi.AdvancePolicyRequest(
                null, "GLOBAL", null, "AUTO", "MONTHLY", new BigDecimal("50"),
                1, 0, "2026-08-01", "", true), "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo("ADVANCE_POLICY_EXISTS"));
    }

    private WorkforceAdvancePolicy policy(String scopeType, String scopeId, String mode,
                                          String frequency, int version) {
        return new WorkforceAdvancePolicy(scopeType, scopeId, mode, frequency,
                new BigDecimal("50"), 1, 0, true, version, LocalDate.of(2026, 1, 1), null);
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
