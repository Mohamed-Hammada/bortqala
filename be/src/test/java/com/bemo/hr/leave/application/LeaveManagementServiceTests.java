package com.bemo.hr.leave.application;

import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.leave.api.LeaveManagementApi;
import com.bemo.hr.leave.domain.LeaveBalanceAccount;
import com.bemo.hr.leave.domain.LeaveRequest;
import com.bemo.hr.leave.domain.LeaveRequestStatus;
import com.bemo.hr.leave.domain.LeaveType;
import com.bemo.hr.leave.infrastructure.LeaveBalanceAccountRepository;
import com.bemo.hr.leave.infrastructure.LeaveRequestRepository;
import com.bemo.hr.leave.infrastructure.LeaveTypeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaveManagementServiceTests {

    private LeaveTypeRepository leaveTypeRepository;
    private LeaveBalanceAccountRepository balanceAccountRepository;
    private LeaveRequestRepository leaveRequestRepository;
    private EmployeeRepository employeeRepository;
    private LeaveManagementService leaveService;

    @BeforeEach
    void setUp() {
        leaveTypeRepository = mock(LeaveTypeRepository.class);
        balanceAccountRepository = mock(LeaveBalanceAccountRepository.class);
        leaveRequestRepository = mock(LeaveRequestRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        leaveService = new LeaveManagementService(leaveTypeRepository, balanceAccountRepository, leaveRequestRepository, employeeRepository);
    }

    @Test
    void submitsLeaveRequestAndReservesBalance() {
        Employee emp = new Employee("EMP-001", "Hassan Mahmoud", "DEV-01", "cat-1",
                EmploymentType.FIXED, new BigDecimal("10000"), LocalDate.of(2026, 1, 1), null, true);
        LeaveType type = new LeaveType("ANNUAL", "إجازة اعتيادية", "Annual Leave", true, false, 30);
        LeaveBalanceAccount bal = new LeaveBalanceAccount("emp-1", "type-1", 2026, new BigDecimal("21.0"), BigDecimal.ZERO);

        when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(emp));
        when(leaveTypeRepository.findById("type-1")).thenReturn(Optional.of(type));
        when(leaveRequestRepository.findOverlappingRequests(any(), any(), any())).thenReturn(Collections.emptyList());
        when(balanceAccountRepository.findByEmployeeIdAndLeaveTypeIdAndYear("emp-1", "type-1", 2026)).thenReturn(Optional.of(bal));
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LeaveManagementApi.SubmitLeaveRequest request = new LeaveManagementApi.SubmitLeaveRequest(
                "emp-1",
                "type-1",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 5),
                "Annual vacation"
        );

        LeaveManagementApi.LeaveRequestResponse resp = leaveService.submitLeaveRequest(request);
        assertThat(resp).isNotNull();
        assertThat(resp.totalDays()).isEqualByComparingTo(new BigDecimal("5.0"));
        assertThat(resp.status()).isEqualTo(LeaveRequestStatus.PENDING_APPROVAL);
        assertThat(bal.getPendingDays()).isEqualByComparingTo(new BigDecimal("5.0"));
        assertThat(bal.getRemainingDays()).isEqualByComparingTo(new BigDecimal("16.0"));
    }

    @Test
    void blocksOverlappingLeaveRequest() {
        Employee emp = new Employee("EMP-001", "Hassan Mahmoud", "DEV-01", "cat-1",
                EmploymentType.FIXED, new BigDecimal("10000"), LocalDate.of(2026, 1, 1), null, true);
        LeaveType type = new LeaveType("ANNUAL", "إجازة اعتيادية", "Annual Leave", true, false, 30);
        LeaveRequest existing = new LeaveRequest("LR-001", "emp-1", "type-1", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), new BigDecimal("5.0"), "Trip");

        when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(emp));
        when(leaveTypeRepository.findById("type-1")).thenReturn(Optional.of(type));
        when(leaveRequestRepository.findOverlappingRequests(any(), any(), any())).thenReturn(Collections.singletonList(existing));

        LeaveManagementApi.SubmitLeaveRequest request = new LeaveManagementApi.SubmitLeaveRequest(
                "emp-1",
                "type-1",
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 7),
                "Conflict"
        );

        assertThatThrownBy(() -> leaveService.submitLeaveRequest(request))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("LEAVE_OVERLAP"));
    }

    @Test
    void approvesLeaveRequestAndConsumesBalance() {
        LeaveType type = new LeaveType("ANNUAL", "إجازة اعتيادية", "Annual Leave", true, false, 30);
        LeaveRequest req = new LeaveRequest("LR-001", "emp-1", "type-1", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), new BigDecimal("5.0"), "Trip");
        LeaveBalanceAccount bal = new LeaveBalanceAccount("emp-1", "type-1", 2026, new BigDecimal("21.0"), BigDecimal.ZERO);
        bal.reserveDays(new BigDecimal("5.0"));

        when(leaveRequestRepository.findById("req-1")).thenReturn(Optional.of(req));
        when(leaveTypeRepository.findById("type-1")).thenReturn(Optional.of(type));
        when(balanceAccountRepository.findByEmployeeIdAndLeaveTypeIdAndYear("emp-1", "type-1", 2026)).thenReturn(Optional.of(bal));
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LeaveManagementApi.LeaveRequestResponse resp = leaveService.approveLeaveRequest("req-1", "user-mgr");
        assertThat(resp.status()).isEqualTo(LeaveRequestStatus.APPROVED);
        assertThat(bal.getUsedDays()).isEqualByComparingTo(new BigDecimal("5.0"));
        assertThat(bal.getPendingDays()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bal.getRemainingDays()).isEqualByComparingTo(new BigDecimal("16.0"));
    }
}
