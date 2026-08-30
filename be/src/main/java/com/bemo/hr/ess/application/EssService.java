package com.bemo.hr.ess.application;

import com.bemo.hr.attendance.domain.AttendanceSelfiePunch;
import com.bemo.hr.attendance.infrastructure.AttendanceSelfiePunchRepository;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.ess.api.EssApi;
import com.bemo.hr.leave.api.LeaveManagementApi;
import com.bemo.hr.leave.application.LeaveManagementService;
import com.bemo.hr.leave.domain.LeaveBalanceAccount;
import com.bemo.hr.leave.domain.LeaveRequest;
import com.bemo.hr.leave.domain.LeaveType;
import com.bemo.hr.leave.infrastructure.LeaveBalanceAccountRepository;
import com.bemo.hr.leave.infrastructure.LeaveRequestRepository;
import com.bemo.hr.leave.infrastructure.LeaveTypeRepository;
import com.bemo.hr.payroll.domain.SalaryPayment;
import com.bemo.hr.payroll.domain.SalaryPaymentExplanation;
import com.bemo.hr.payroll.domain.SalaryPaymentExplanationRepository;
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.workforce.WorkforceAdvance;
import com.bemo.hr.workforce.WorkforceAdvanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EssService {

    private static final Logger log = LoggerFactory.getLogger(EssService.class);

    private final AppUserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceCategoryRepository categoryRepository;
    private final SalaryPaymentRepository salaryPaymentRepository;
    private final SalaryPaymentExplanationRepository explanationRepository;
    private final LeaveBalanceAccountRepository balanceAccountRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveManagementService leaveManagementService;
    private final WorkforceAdvanceRepository advanceRepository;
    private final AttendanceSelfiePunchRepository selfiePunchRepository;

    public EssService(AppUserRepository userRepository,
                      EmployeeRepository employeeRepository,
                      AttendanceCategoryRepository categoryRepository,
                      SalaryPaymentRepository salaryPaymentRepository,
                      SalaryPaymentExplanationRepository explanationRepository,
                      LeaveBalanceAccountRepository balanceAccountRepository,
                      LeaveRequestRepository leaveRequestRepository,
                      LeaveTypeRepository leaveTypeRepository,
                      LeaveManagementService leaveManagementService,
                      WorkforceAdvanceRepository advanceRepository,
                      AttendanceSelfiePunchRepository selfiePunchRepository) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.categoryRepository = categoryRepository;
        this.salaryPaymentRepository = salaryPaymentRepository;
        this.explanationRepository = explanationRepository;
        this.balanceAccountRepository = balanceAccountRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveManagementService = leaveManagementService;
        this.advanceRepository = advanceRepository;
        this.selfiePunchRepository = selfiePunchRepository;
    }

    public Employee resolveCurrentEmployee(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessRuleException("User account is not authenticated.", "ESS_EMPLOYEE_PROFILE_NOT_LINKED", HttpStatus.UNAUTHORIZED);
        }
        Optional<AppUser> userOpt = userRepository.findByUsernameIgnoreCase(username);
        if (userOpt.isPresent() && userOpt.get().getEmployeeId() != null && !userOpt.get().getEmployeeId().isBlank()) {
            return employeeRepository.findById(userOpt.get().getEmployeeId())
                    .orElseThrow(() -> new BusinessRuleException("Linked employee profile not found.", "ESS_EMPLOYEE_PROFILE_NOT_LINKED", HttpStatus.NOT_FOUND));
        }

        // Fallback: match by employeeCode or deviceUserId
        return employeeRepository.findByEmployeeCodeIgnoreCase(username)
                .or(() -> employeeRepository.findByDeviceUserId(username))
                .orElseThrow(() -> new BusinessRuleException("User account is not linked to an employee profile.", "ESS_EMPLOYEE_PROFILE_NOT_LINKED", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public EssApi.ProfileResponse getProfile(String username) {
        Employee employee = resolveCurrentEmployee(username);
        String categoryName = categoryRepository.findById(employee.getCategoryId())
                .map(c -> c.getName())
                .orElse(employee.getCategoryId());

        int currentYear = LocalDate.now().getYear();
        BigDecimal annualRemaining = balanceAccountRepository.findByEmployeeIdAndYear(employee.getId(), currentYear)
                .stream()
                .map(LeaveBalanceAccount::getRemainingDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingLeaves = leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()).stream()
                .filter(r -> "PENDING_APPROVAL".equals(r.getStatus().name()))
                .count();

        long pendingAdvances = advanceRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()).stream()
                .filter(a -> "REQUESTED".equals(a.getStatus()))
                .count();

        List<AttendanceSelfiePunch> recentPunches = selfiePunchRepository.findByEmployeeIdOrderByPunchedAtDesc(employee.getId());
        int currentMonthPunchesCount = recentPunches.size();
        String lastPunchTime = null;
        String lastPunchType = null;
        if (!recentPunches.isEmpty()) {
            AttendanceSelfiePunch last = recentPunches.get(0);
            lastPunchTime = Instant.ofEpochMilli(last.getPunchedAt()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            lastPunchType = "SELFIE";
        }

        return new EssApi.ProfileResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getCategoryId(),
                categoryName,
                employee.getEmploymentType() != null ? employee.getEmploymentType().name() : "FULL_TIME",
                employee.getActiveFrom(),
                employee.getBaseSalary(),
                annualRemaining,
                pendingLeaves,
                pendingAdvances,
                currentMonthPunchesCount,
                lastPunchTime,
                lastPunchType
        );
    }

    @Transactional(readOnly = true)
    public List<EssApi.PayslipSummaryResponse> listMyPayslips(String username, Integer year) {
        Employee employee = resolveCurrentEmployee(username);
        List<SalaryPayment> payments = salaryPaymentRepository.findByEmployeeIdOrderByPeriodStartDesc(employee.getId());

        return payments.stream()
                .filter(p -> "PAID".equalsIgnoreCase(p.getPaymentStatus().name()))
                .filter(p -> year == null || p.getPeriodYear() == year)
                .map(p -> new EssApi.PayslipSummaryResponse(
                        p.getId(),
                        p.getPeriodYear(),
                        p.getPeriodMonth(),
                        p.getPeriodKind(),
                        p.getPeriodStart(),
                        p.getPeriodEnd(),
                        p.getGrossAmount(),
                        p.getAdvancesDeducted().add(p.getOtherDeductions()),
                        p.getNetAmount(),
                        p.getPaymentStatus().name(),
                        p.getPaidAt() != null ? p.getPaidAt().toEpochMilli() : null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public EssApi.PayslipDetailResponse getPayslipDetail(String username, String paymentId) {
        Employee employee = resolveCurrentEmployee(username);
        SalaryPayment payment = salaryPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessRuleException("Payslip not found.", "ESS_PAYSLIP_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!payment.getEmployeeId().equals(employee.getId())) {
            throw new BusinessRuleException("Payslip not found.", "ESS_PAYSLIP_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        List<SalaryPaymentExplanation> explanations = explanationRepository.findBySalaryPaymentIdOrderByCreatedAtAsc(paymentId);
        List<EssApi.ExplanationItem> items = explanations.stream()
                .map(e -> new EssApi.ExplanationItem(
                        e.getComponentType(),
                        e.getExplanationTextEn() != null ? e.getExplanationTextEn() : e.getComponentType(),
                        e.getCalculatedAmount(),
                        e.getFormula()
                ))
                .toList();

        BigDecimal totalDeductions = payment.getAdvancesDeducted().add(payment.getOtherDeductions());

        return new EssApi.PayslipDetailResponse(
                payment.getId(),
                payment.getPeriodYear(),
                payment.getPeriodMonth(),
                payment.getPeriodKind(),
                payment.getPeriodStart(),
                payment.getPeriodEnd(),
                payment.getGrossAmount(), // Base gross
                payment.getGrossAmount(),
                totalDeductions,
                payment.getNetAmount(),
                payment.getPaymentStatus().name(),
                payment.getPaidAt() != null ? payment.getPaidAt().toEpochMilli() : null,
                30,
                30,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                payment.getOtherDeductions(),
                payment.getAdvancesDeducted(),
                payment.getBonuses(),
                BigDecimal.ZERO,
                items
        );
    }

    @Transactional
    public EssApi.LeaveResponse submitLeave(String username, EssApi.LeaveSubmitRequest request) {
        Employee employee = resolveCurrentEmployee(username);
        LeaveManagementApi.LeaveRequestResponse res = leaveManagementService.submitLeaveRequest(
                new LeaveManagementApi.SubmitLeaveRequest(
                        employee.getId(),
                        request.leaveTypeId(),
                        request.startDate(),
                        request.endDate(),
                        request.reason()
                )
        );

        return new EssApi.LeaveResponse(
                res.id(),
                res.requestNumber(),
                res.leaveTypeId(),
                res.leaveTypeName(),
                res.startDate(),
                res.endDate(),
                res.totalDays(),
                res.reason(),
                res.status().name(),
                System.currentTimeMillis()
        );
    }

    @Transactional(readOnly = true)
    public List<EssApi.LeaveResponse> listMyLeaves(String username) {
        Employee employee = resolveCurrentEmployee(username);
        List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId());
        Map<String, String> typeNames = leaveTypeRepository.findAll().stream()
                .collect(Collectors.toMap(LeaveType::getId, LeaveType::getNameAr, (a, b) -> a));

        return requests.stream()
                .map(r -> new EssApi.LeaveResponse(
                        r.getId(),
                        r.getRequestNumber(),
                        r.getLeaveTypeId(),
                        typeNames.getOrDefault(r.getLeaveTypeId(), r.getLeaveTypeId()),
                        r.getStartDate(),
                        r.getEndDate(),
                        r.getTotalDays(),
                        r.getReason(),
                        r.getStatus().name(),
                        r.getCreatedAt()
                ))
                .toList();
    }


    @Transactional
    public EssApi.AdvanceResponse submitAdvance(String username, EssApi.AdvanceSubmitRequest request) {
        Employee employee = resolveCurrentEmployee(username);
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new BusinessRuleException("Advance amount must be greater than zero.", "ESS_ADVANCE_AMOUNT_INVALID", HttpStatus.BAD_REQUEST);
        }
        if (request.totalInstallments() < 1 || request.totalInstallments() > 36) {
            throw new BusinessRuleException("Installments must be between 1 and 36.", "ESS_ADVANCE_INSTALLMENTS_INVALID", HttpStatus.BAD_REQUEST);
        }

        BigDecimal installmentAmount = request.amount().divide(BigDecimal.valueOf(request.totalInstallments()), 2, RoundingMode.HALF_UP);
        String firstDate = request.firstInstallmentDate() != null && !request.firstInstallmentDate().isBlank()
                ? request.firstInstallmentDate()
                : LocalDate.now().plusMonths(1).withDayOfMonth(1).toString();

        WorkforceAdvance advance = new WorkforceAdvance(
                "EMPLOYEE",
                null,
                null,
                employee.getId(),
                request.amount(),
                "INSTALLMENT",
                request.totalInstallments(),
                installmentAmount,
                "MONTHLY",
                new BigDecimal("50.00"),
                request.reason(),
                firstDate,
                "AUTO",
                0
        );

        advance.updateStatus("REQUESTED");
        WorkforceAdvance saved = advanceRepository.save(advance);

        return new EssApi.AdvanceResponse(
                saved.getId(),
                saved.getAmount(),
                saved.getTotalInstallments(),
                saved.getInstallmentAmount(),
                saved.getRemainingBalance(),
                saved.getStatus(),
                saved.getFirstInstallmentDate(),
                saved.getReason(),
                saved.getCreatedAt() != null ? saved.getCreatedAt().toEpochMilli() : System.currentTimeMillis()
        );
    }

    @Transactional(readOnly = true)
    public List<EssApi.AdvanceResponse> listMyAdvances(String username) {
        Employee employee = resolveCurrentEmployee(username);
        List<WorkforceAdvance> list = advanceRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId());

        return list.stream()
                .map(a -> new EssApi.AdvanceResponse(
                        a.getId(),
                        a.getAmount(),
                        a.getTotalInstallments(),
                        a.getInstallmentAmount(),
                        a.getRemainingBalance(),
                        a.getStatus(),
                        a.getFirstInstallmentDate(),
                        a.getReason(),
                        a.getCreatedAt() != null ? a.getCreatedAt().toEpochMilli() : null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EssApi.AttendanceRecordResponse> listMyAttendance(String username) {
        Employee employee = resolveCurrentEmployee(username);
        List<AttendanceSelfiePunch> selfiePunches = selfiePunchRepository.findByEmployeeIdOrderByPunchedAtDesc(employee.getId());

        return selfiePunches.stream()
                .map(p -> {
                    LocalDate date = Instant.ofEpochMilli(p.getPunchedAt()).atZone(ZoneId.systemDefault()).toLocalDate();
                    String time = Instant.ofEpochMilli(p.getPunchedAt()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    return new EssApi.AttendanceRecordResponse(
                            date,
                            time,
                            null,
                            "PRESENT",
                            new BigDecimal("8.00")
                    );
                })
                .toList();
    }
}
