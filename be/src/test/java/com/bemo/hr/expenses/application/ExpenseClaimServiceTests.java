package com.bemo.hr.expenses.application;

import com.bemo.hr.expenses.api.ExpenseClaimApi;
import com.bemo.hr.expenses.domain.ExpenseClaim;
import com.bemo.hr.expenses.infrastructure.ExpenseClaimRepository;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.operations.EmployeeAdvanceEntry;
import com.bemo.hr.expenses.infrastructure.ExpenseReimbursementEntryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseClaimServiceTests {

    @Mock private ExpenseClaimRepository expenseClaimRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ExpenseReimbursementEntryRepository advanceEntryRepository;

    private ExpenseClaimService service;

    private final Employee alice = buildEmployee("alice", "A", "Alice");
    private final Employee bob = buildEmployee("bob", "B", "Bob");

    @BeforeEach
    void setUp() {
        service = new ExpenseClaimService(expenseClaimRepository, employeeRepository, advanceEntryRepository);
        setField(service, "sodEnabled", "true");
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_and_submit_happy_path() {
        when(employeeRepository.findByDeviceUserId("alice")).thenReturn(Optional.of(alice));
        when(expenseClaimRepository.save(any(ExpenseClaim.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new ExpenseClaimApi.CreateClaimRequest("MEAL", LocalDate.of(2026, 8, 15),
                new BigDecimal("50.00"), null, "Lunch with client", null, null, null);
        ExpenseClaim claim = service.create("alice", req);

        assertThat(claim.getStatus()).isEqualTo("DRAFT");
        assertThat(claim.getCategory()).isEqualTo("MEAL");
        assertThat(claim.getEmployeeId()).isEqualTo(alice.getId());

        claim.submit();
        assertThat(claim.getStatus()).isEqualTo("SUBMITTED");
    }

    @Test
    void approve_sets_approver_and_blocks_self_approval() {
        when(employeeRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        ExpenseClaim claim = buildClaim(alice.getId(), ExpenseClaim.Status.SUBMITTED);
        when(expenseClaimRepository.findById("clm-1")).thenReturn(Optional.of(claim));
        when(expenseClaimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null));

        assertThatThrownBy(() -> service.approve("clm-1", null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(code(ex)).isEqualTo("EXPENSE_SELF_APPROVAL"));
    }

    @Test
    void approve_from_submitted_blocks_non_submitted() {
        when(employeeRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        ExpenseClaim claim = buildClaim(alice.getId(), ExpenseClaim.Status.DRAFT);
        when(expenseClaimRepository.findById("clm-1")).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.approve("clm-1", null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(code(ex)).isEqualTo("EXPENSE_INVALID_STATE"));
    }

    @Test
    void reject_requires_no_special_validation_and_sets_status() {
        when(employeeRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        ExpenseClaim claim = buildClaim(alice.getId(), ExpenseClaim.Status.SUBMITTED);
        when(expenseClaimRepository.findById("clm-1")).thenReturn(Optional.of(claim));
        when(expenseClaimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hr-admin", null));

        ExpenseClaim result = service.reject("clm-1", "Missing receipt");
        assertThat(result.getStatus()).isEqualTo("REJECTED");
        assertThat(result.getDecisionNote()).isEqualTo("Missing receipt");
        assertThat(result.getApproverId()).isEqualTo("hr-admin");
    }

    @Test
    void reimburse_only_from_approved_creates_advance_entry() {
        when(employeeRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        ExpenseClaim claim = buildClaim(alice.getId(), ExpenseClaim.Status.APPROVED);
        claim.setAmount(new BigDecimal("100.00"));
        when(expenseClaimRepository.findById("clm-1")).thenReturn(Optional.of(claim));
        when(expenseClaimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(advanceEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("finance", null));

        ExpenseClaim result = service.reimburse("clm-1", "PO-42");
        assertThat(result.getStatus()).isEqualTo("REIMBURSED");
        assertThat(result.getReimbursementReference()).isEqualTo("PO-42");

        ArgumentCaptor<EmployeeAdvanceEntry> captor = ArgumentCaptor.forClass(EmployeeAdvanceEntry.class);
        verify(advanceEntryRepository).save(captor.capture());
        EmployeeAdvanceEntry entry = captor.getValue();
        assertThat(entry.getEmployeeId()).isEqualTo(alice.getId());
        assertThat(entry.getAmountDelta()).isEqualByComparingTo(new BigDecimal("-100.00"));
        assertThat(entry.getEntryType()).isEqualTo("EXPENSE_REIMBURSEMENT");
    }

    @Test
    void reimburse_from_non_approved_throws() {
        ExpenseClaim claim = buildClaim(alice.getId(), ExpenseClaim.Status.SUBMITTED);
        when(expenseClaimRepository.findById("clm-1")).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.reimburse("clm-1", "ref"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(code(ex)).isEqualTo("EXPENSE_INVALID_STATE"));
    }

    @Test
    void listMine_resolves_employee_from_username() {
        when(employeeRepository.findByDeviceUserId("alice")).thenReturn(Optional.of(alice));
        when(expenseClaimRepository.findByEmployeeIdOrderByCreatedAtDesc(alice.getId()))
                .thenReturn(List.of(buildClaim(alice.getId(), ExpenseClaim.Status.DRAFT)));

        List<ExpenseClaim> claims = service.listMine("alice");
        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).getEmployeeId()).isEqualTo(alice.getId());
    }

    @Test
    void other_employee_cannot_access_own_claim() {
        when(employeeRepository.findByDeviceUserId("bob")).thenReturn(Optional.of(bob));
        ExpenseClaim claim = buildClaim(alice.getId(), ExpenseClaim.Status.DRAFT);
        when(expenseClaimRepository.findById("clm-1")).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.submit("bob", "clm-1"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(code(ex)).isEqualTo("EXPENSE_NOT_OWN"));
    }

    @Test
    void username_without_employee_record_throws() {
        when(employeeRepository.findByDeviceUserId("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.listMine("ghost"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(code(ex)).isEqualTo("EXPENSE_NO_EMPLOYEE_LINKED"));
    }

    @Test
    void receipt_too_large_throws() {
        when(employeeRepository.findByDeviceUserId("alice")).thenReturn(Optional.of(alice));
        when(expenseClaimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new ExpenseClaimApi.CreateClaimRequest("MEAL", LocalDate.of(2026, 8, 15),
                new BigDecimal("10.00"), null, "Snack", "receipt.jpg", "image/jpeg", 6_000_000L);
        assertThatThrownBy(() -> service.create("alice", req))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(code(ex)).isEqualTo("EXPENSE_RECEIPT_TOO_LARGE"));
    }

    @Test
    void receipt_wrong_type_throws() {
        when(employeeRepository.findByDeviceUserId("alice")).thenReturn(Optional.of(alice));
        when(expenseClaimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new ExpenseClaimApi.CreateClaimRequest("MEAL", LocalDate.of(2026, 8, 15),
                new BigDecimal("10.00"), null, "Snack", "video.mp4", "video/mp4", 1_000_000L);
        assertThatThrownBy(() -> service.create("alice", req))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(code(ex)).isEqualTo("EXPENSE_RECEIPT_INVALID_TYPE"));
    }

    private ExpenseClaim buildClaim(String employeeId, ExpenseClaim.Status status) {
        ExpenseClaim claim = new ExpenseClaim(employeeId, ExpenseClaim.Category.MEAL,
                LocalDate.of(2026, 8, 15), new BigDecimal("25.00"), "EGP", "Lunch");
        claim.setStatus(status.name());
        claim.setId("clm-1");
        return claim;
    }

    private Employee buildEmployee(String deviceUserId, String id, String name) {
        Employee e = new Employee(id.toUpperCase(), name, deviceUserId, "CAT-1",
                com.bemo.hr.employee.domain.EmploymentType.FIXED,
                java.time.LocalDate.of(2024, 1, 1), null, true);
        return e;
    }


    private static String code(Throwable ex) {
        return ((com.bemo.hr.shared.domain.BusinessRuleException) ex).getCode();
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
