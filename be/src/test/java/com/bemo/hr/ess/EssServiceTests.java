package com.bemo.hr.ess;

import com.bemo.hr.attendance.domain.AttendanceSelfiePunch;
import com.bemo.hr.attendance.infrastructure.AttendanceSelfiePunchRepository;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.ess.api.EssApi;
import com.bemo.hr.ess.application.EssService;
import com.bemo.hr.leave.api.LeaveManagementApi;
import com.bemo.hr.leave.application.LeaveManagementService;
import com.bemo.hr.leave.domain.LeaveBalanceAccount;
import com.bemo.hr.leave.infrastructure.LeaveBalanceAccountRepository;
import com.bemo.hr.leave.infrastructure.LeaveRequestRepository;
import com.bemo.hr.leave.infrastructure.LeaveTypeRepository;
import com.bemo.hr.payroll.domain.PaymentStatus;
import com.bemo.hr.payroll.domain.SalaryPayment;
import com.bemo.hr.payroll.domain.SalaryPaymentExplanationRepository;
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.workforce.WorkforceAdvance;
import com.bemo.hr.workforce.WorkforceAdvanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EssServiceTests {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private AttendanceCategoryRepository categoryRepository;
    @Mock
    private SalaryPaymentRepository salaryPaymentRepository;
    @Mock
    private SalaryPaymentExplanationRepository explanationRepository;
    @Mock
    private LeaveBalanceAccountRepository balanceAccountRepository;
    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private LeaveTypeRepository leaveTypeRepository;
    @Mock
    private LeaveManagementService leaveManagementService;
    @Mock
    private WorkforceAdvanceRepository advanceRepository;
    @Mock
    private AttendanceSelfiePunchRepository selfiePunchRepository;

    @InjectMocks
    private EssService essService;

    private Employee employee;
    private AppUser appUser;

    @BeforeEach
    void setUp() {
        employee = new Employee("EMP-001", "Ahmed Ali", "DEV-01", "CAT-1",
                EmploymentType.FIXED, new BigDecimal("10000.00"), LocalDate.of(2025, 1, 1), null, true);

        appUser = new AppUser("test-app", "ahmed", "Ahmed Ali", "hash", Set.of(), Set.of(), true, true);
        appUser.setEmployeeId(employee.getId());
    }

    @Test
    void shouldResolveProfileSuccessfully() {
        when(userRepository.findByUsernameIgnoreCase("ahmed")).thenReturn(Optional.of(appUser));
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(categoryRepository.findById("CAT-1")).thenReturn(Optional.empty());
        when(balanceAccountRepository.findByEmployeeIdAndYear(eq(employee.getId()), anyInt()))
                .thenReturn(List.of(new LeaveBalanceAccount(employee.getId(), "LT-1", 2026, new BigDecimal("21.0"), BigDecimal.ZERO)));
        when(leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId())).thenReturn(List.of());
        when(advanceRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId())).thenReturn(List.of());
        when(selfiePunchRepository.findByEmployeeIdOrderByPunchedAtDesc(employee.getId())).thenReturn(List.of());

        EssApi.ProfileResponse profile = essService.getProfile("ahmed");

        assertThat(profile.fullName()).isEqualTo("Ahmed Ali");
        assertThat(profile.employeeCode()).isEqualTo("EMP-001");
        assertThat(profile.annualLeaveRemainingDays()).isEqualByComparingTo("21.0");
    }

    @Test
    void shouldBlockAccessToOtherEmployeesPayslips_AC1() {
        when(userRepository.findByUsernameIgnoreCase("ahmed")).thenReturn(Optional.of(appUser));
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        SalaryPayment otherPayment = mock(SalaryPayment.class);
        when(otherPayment.getEmployeeId()).thenReturn("OTHER-EMP-ID");
        when(salaryPaymentRepository.findById("pay-999")).thenReturn(Optional.of(otherPayment));

        assertThatThrownBy(() -> essService.getPayslipDetail("ahmed", "pay-999"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Payslip not found");
    }

    @Test
    void shouldReturnExactFrozenPayslipSnapshot_AC2() {
        when(userRepository.findByUsernameIgnoreCase("ahmed")).thenReturn(Optional.of(appUser));
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        SalaryPayment payment = mock(SalaryPayment.class);
        when(payment.getId()).thenReturn("pay-100");
        when(payment.getEmployeeId()).thenReturn(employee.getId());
        when(payment.getPeriodYear()).thenReturn(2026);
        when(payment.getPeriodMonth()).thenReturn(8);
        when(payment.getPeriodKind()).thenReturn("MONTHLY");
        when(payment.getPeriodStart()).thenReturn(LocalDate.of(2026, 8, 1));
        when(payment.getPeriodEnd()).thenReturn(LocalDate.of(2026, 8, 31));
        when(payment.getGrossAmount()).thenReturn(new BigDecimal("11000.00"));
        when(payment.getAdvancesDeducted()).thenReturn(new BigDecimal("1000.00"));
        when(payment.getOtherDeductions()).thenReturn(BigDecimal.ZERO);
        when(payment.getBonuses()).thenReturn(BigDecimal.ZERO);
        when(payment.getNetAmount()).thenReturn(new BigDecimal("10000.00"));
        when(payment.getPaymentStatus()).thenReturn(PaymentStatus.PAID);
        when(payment.getPaidAt()).thenReturn(Instant.now());

        when(salaryPaymentRepository.findById("pay-100")).thenReturn(Optional.of(payment));
        when(explanationRepository.findBySalaryPaymentIdOrderByCreatedAtAsc("pay-100")).thenReturn(List.of());

        EssApi.PayslipDetailResponse detail = essService.getPayslipDetail("ahmed", "pay-100");

        assertThat(detail.netPay()).isEqualByComparingTo("10000.00");
        assertThat(detail.grossTotal()).isEqualByComparingTo("11000.00");
        assertThat(detail.advanceDeductions()).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldSubmitLeaveRequestThroughService_AC3() {
        when(userRepository.findByUsernameIgnoreCase("ahmed")).thenReturn(Optional.of(appUser));
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        LeaveManagementApi.LeaveRequestResponse mockLeaveRes = new LeaveManagementApi.LeaveRequestResponse(
                "req-1", "LR-2026-001", employee.getId(), "Ahmed Ali", "LT-1", "ANNUAL", "Annual Leave",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), new BigDecimal("5.0"),
                com.bemo.hr.leave.domain.LeaveRequestStatus.PENDING_APPROVAL, "Personal", null, null, null,
                1000L, 1000L, 0L
        );

        when(leaveManagementService.submitLeaveRequest(any(LeaveManagementApi.SubmitLeaveRequest.class)))
                .thenReturn(mockLeaveRes);

        EssApi.LeaveResponse res = essService.submitLeave("ahmed", new EssApi.LeaveSubmitRequest(
                "LT-1", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), "Personal"
        ));

        assertThat(res.requestNumber()).isEqualTo("LR-2026-001");
        assertThat(res.status()).isEqualTo("PENDING_APPROVAL");
        assertThat(res.totalDays()).isEqualByComparingTo("5.0");
    }


    @Test
    void shouldSubmitAdvanceInRequestedStatus_AC4() {
        when(userRepository.findByUsernameIgnoreCase("ahmed")).thenReturn(Optional.of(appUser));
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(advanceRepository.save(any(WorkforceAdvance.class))).thenAnswer(inv -> inv.getArgument(0));

        EssApi.AdvanceResponse res = essService.submitAdvance("ahmed", new EssApi.AdvanceSubmitRequest(
                new BigDecimal("6000.00"), 6, "2026-10-01", "Family need"
        ));

        assertThat(res.amount()).isEqualByComparingTo("6000.00");
        assertThat(res.totalInstallments()).isEqualTo(6);
        assertThat(res.installmentAmount()).isEqualByComparingTo("1000.00");
        assertThat(res.status()).isEqualTo("REQUESTED");
    }
}
