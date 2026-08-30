package com.bemo.hr.leave.application;

import com.bemo.hr.employee.domain.Employee;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LeaveManagementService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceAccountRepository balanceAccountRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveManagementService(LeaveTypeRepository leaveTypeRepository,
                                  LeaveBalanceAccountRepository balanceAccountRepository,
                                  LeaveRequestRepository leaveRequestRepository,
                                  EmployeeRepository employeeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
        this.balanceAccountRepository = balanceAccountRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaveManagementApi.LeaveTypeResponse> listLeaveTypes() {
        return leaveTypeRepository.findAllByOrderByCodeAsc().stream()
                .map(this::toTypeResponse)
                .toList();
    }

    @Transactional
    public LeaveManagementApi.LeaveTypeResponse createLeaveType(LeaveManagementApi.CreateLeaveTypeRequest request) {
        if (leaveTypeRepository.existsByCode(request.code())) {
            throw new BusinessRuleException("Leave type code already exists", "LEAVE_TYPE_CODE_EXISTS", HttpStatus.BAD_REQUEST);
        }
        LeaveType type = new LeaveType(
                request.code(),
                request.nameAr(),
                request.nameEn(),
                request.paid(),
                request.requiresAttachment(),
                request.maxConsecutiveDays()
        );
        LeaveType saved = leaveTypeRepository.save(type);
        log.info("Leave type created: {}", saved.getCode());
        return toTypeResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaveManagementApi.LeaveBalanceResponse> listBalances(String employeeId, Integer year) {
        int targetYear = (year != null && year > 0) ? year : LocalDate.now().getYear();
        List<LeaveBalanceAccount> accounts;
        if (employeeId != null && !employeeId.isBlank()) {
            accounts = balanceAccountRepository.findByEmployeeIdAndYear(employeeId, targetYear);
        } else {
            accounts = balanceAccountRepository.findByYear(targetYear);
        }

        Map<String, Employee> empMap = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        Map<String, LeaveType> typeMap = leaveTypeRepository.findAll().stream()
                .collect(Collectors.toMap(LeaveType::getId, t -> t, (a, b) -> a));

        return accounts.stream()
                .map(acc -> toBalanceResponse(acc, empMap.get(acc.getEmployeeId()), typeMap.get(acc.getLeaveTypeId())))
                .toList();
    }

    @Transactional
    public LeaveManagementApi.LeaveBalanceResponse adjustBalance(LeaveManagementApi.AdjustBalanceRequest request) {
        int year = request.year() > 0 ? request.year() : LocalDate.now().getYear();
        Employee emp = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new BusinessRuleException("Employee not found", "EMPLOYEE_NOT_FOUND", HttpStatus.NOT_FOUND));
        LeaveType type = leaveTypeRepository.findById(request.leaveTypeId())
                .orElseThrow(() -> new BusinessRuleException("Leave type not found", "LEAVE_TYPE_NOT_FOUND", HttpStatus.NOT_FOUND));

        LeaveBalanceAccount account = balanceAccountRepository.findByEmployeeIdAndLeaveTypeIdAndYear(request.employeeId(), request.leaveTypeId(), year)
                .orElseGet(() -> new LeaveBalanceAccount(request.employeeId(), request.leaveTypeId(), year, request.entitledDays(), request.carriedOverDays()));

        LeaveBalanceAccount saved = balanceAccountRepository.save(account);
        return toBalanceResponse(saved, emp, type);
    }

    @Transactional(readOnly = true)
    public List<LeaveManagementApi.LeaveRequestResponse> listRequests(String employeeId, LeaveRequestStatus status) {
        List<LeaveRequest> list;
        if (employeeId != null && !employeeId.isBlank()) {
            list = leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
        } else if (status != null) {
            list = leaveRequestRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = leaveRequestRepository.findAllByOrderByCreatedAtDesc();
        }

        Map<String, Employee> empMap = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        Map<String, LeaveType> typeMap = leaveTypeRepository.findAll().stream()
                .collect(Collectors.toMap(LeaveType::getId, t -> t, (a, b) -> a));

        return list.stream()
                .map(req -> toRequestResponse(req, empMap.get(req.getEmployeeId()), typeMap.get(req.getLeaveTypeId())))
                .toList();
    }

    @Transactional
    public LeaveManagementApi.LeaveRequestResponse submitLeaveRequest(LeaveManagementApi.SubmitLeaveRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new BusinessRuleException("Employee not found", "EMPLOYEE_NOT_FOUND", HttpStatus.NOT_FOUND));
        LeaveType type = leaveTypeRepository.findById(request.leaveTypeId())
                .orElseThrow(() -> new BusinessRuleException("Leave type not found", "LEAVE_TYPE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("End date is before start date", "INVALID_DATE_RANGE", HttpStatus.BAD_REQUEST);
        }

        long dayCount = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        BigDecimal totalDays = BigDecimal.valueOf(dayCount);

        // Overlap validation
        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlappingRequests(request.employeeId(), request.startDate(), request.endDate());
        if (!overlapping.isEmpty()) {
            throw new BusinessRuleException("A leave request already exists for the same period", "LEAVE_OVERLAP", HttpStatus.BAD_REQUEST);
        }

        // Balance validation for paid leave
        int year = request.startDate().getYear();
        LeaveBalanceAccount balance = balanceAccountRepository.findByEmployeeIdAndLeaveTypeIdAndYear(request.employeeId(), request.leaveTypeId(), year)
                .orElseGet(() -> {
                    LeaveBalanceAccount newAcc = new LeaveBalanceAccount(request.employeeId(), request.leaveTypeId(), year, new BigDecimal("21.0"), BigDecimal.ZERO);
                    return balanceAccountRepository.save(newAcc);
                });

        if (type.isPaid() && balance.getRemainingDays().compareTo(totalDays) < 0) {
            throw new BusinessRuleException("Insufficient leave balance", "INSUFFICIENT_LEAVE_BALANCE", HttpStatus.BAD_REQUEST);
        }

        balance.reserveDays(totalDays);
        balanceAccountRepository.save(balance);

        String reqNum = generateRequestNumber();
        LeaveRequest leaveRequest = new LeaveRequest(
                reqNum,
                request.employeeId(),
                request.leaveTypeId(),
                request.startDate(),
                request.endDate(),
                totalDays,
                request.reason()
        );

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request {} submitted for employee {}", saved.getRequestNumber(), request.employeeId());
        return toRequestResponse(saved, employee, type);
    }

    @Transactional
    public LeaveManagementApi.LeaveRequestResponse approveLeaveRequest(String requestId, String approverUserId) {
        LeaveRequest req = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessRuleException("Leave request not found", "LEAVE_REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (req.getStatus() != LeaveRequestStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("Only pending requests can be approved", "INVALID_STATUS_FOR_APPROVAL", HttpStatus.BAD_REQUEST);
        }

        int year = req.getStartDate().getYear();
        balanceAccountRepository.findByEmployeeIdAndLeaveTypeIdAndYear(req.getEmployeeId(), req.getLeaveTypeId(), year)
                .ifPresent(bal -> {
                    bal.consumeDays(req.getTotalDays());
                    balanceAccountRepository.save(bal);
                });

        req.approve(approverUserId != null ? approverUserId : "SYSTEM");
        LeaveRequest saved = leaveRequestRepository.save(req);

        Employee emp = employeeRepository.findById(req.getEmployeeId()).orElse(null);
        LeaveType type = leaveTypeRepository.findById(req.getLeaveTypeId()).orElse(null);
        log.info("Leave request {} approved", saved.getRequestNumber());
        return toRequestResponse(saved, emp, type);
    }

    @Transactional
    public LeaveManagementApi.LeaveRequestResponse rejectLeaveRequest(String requestId, LeaveManagementApi.RejectLeaveRequest request) {
        LeaveRequest req = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessRuleException("Leave request not found", "LEAVE_REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (req.getStatus() != LeaveRequestStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("Only pending requests can be rejected", "INVALID_STATUS_FOR_REJECTION", HttpStatus.BAD_REQUEST);
        }

        int year = req.getStartDate().getYear();
        balanceAccountRepository.findByEmployeeIdAndLeaveTypeIdAndYear(req.getEmployeeId(), req.getLeaveTypeId(), year)
                .ifPresent(bal -> {
                    bal.unreserveDays(req.getTotalDays());
                    balanceAccountRepository.save(bal);
                });

        req.reject(request.rejectionReason());
        LeaveRequest saved = leaveRequestRepository.save(req);

        Employee emp = employeeRepository.findById(req.getEmployeeId()).orElse(null);
        LeaveType type = leaveTypeRepository.findById(req.getLeaveTypeId()).orElse(null);
        log.info("Leave request {} rejected", saved.getRequestNumber());
        return toRequestResponse(saved, emp, type);
    }

    @Transactional
    public LeaveManagementApi.LeaveRequestResponse cancelLeaveRequest(String requestId) {
        LeaveRequest req = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessRuleException("Leave request not found", "LEAVE_REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND));

        int year = req.getStartDate().getYear();
        if (req.getStatus() == LeaveRequestStatus.PENDING_APPROVAL) {
            balanceAccountRepository.findByEmployeeIdAndLeaveTypeIdAndYear(req.getEmployeeId(), req.getLeaveTypeId(), year)
                    .ifPresent(bal -> {
                        bal.unreserveDays(req.getTotalDays());
                        balanceAccountRepository.save(bal);
                    });
        } else if (req.getStatus() == LeaveRequestStatus.APPROVED) {
            balanceAccountRepository.findByEmployeeIdAndLeaveTypeIdAndYear(req.getEmployeeId(), req.getLeaveTypeId(), year)
                    .ifPresent(bal -> {
                        bal.restoreConsumedDays(req.getTotalDays());
                        balanceAccountRepository.save(bal);
                    });
        }

        req.cancel();
        LeaveRequest saved = leaveRequestRepository.save(req);

        Employee emp = employeeRepository.findById(req.getEmployeeId()).orElse(null);
        LeaveType type = leaveTypeRepository.findById(req.getLeaveTypeId()).orElse(null);
        log.info("Leave request {} cancelled", saved.getRequestNumber());
        return toRequestResponse(saved, emp, type);
    }

    private String generateRequestNumber() {
        int year = LocalDate.now().getYear();
        int seq = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.format("LR-%d-%04d", year, seq);
    }

    private LeaveManagementApi.LeaveTypeResponse toTypeResponse(LeaveType t) {
        return new LeaveManagementApi.LeaveTypeResponse(
                t.getId(),
                t.getCode(),
                t.getNameAr(),
                t.getNameEn(),
                t.isPaid(),
                t.isRequiresAttachment(),
                t.getMaxConsecutiveDays(),
                t.getCreatedAt()
        );
    }

    private LeaveManagementApi.LeaveBalanceResponse toBalanceResponse(LeaveBalanceAccount a, Employee emp, LeaveType t) {
        return new LeaveManagementApi.LeaveBalanceResponse(
                a.getId(),
                a.getEmployeeId(),
                emp != null ? emp.getFullName() : a.getEmployeeId(),
                a.getLeaveTypeId(),
                t != null ? t.getCode() : a.getLeaveTypeId(),
                t != null ? t.getNameAr() : a.getLeaveTypeId(),
                a.getYear(),
                a.getEntitledDays(),
                a.getCarriedOverDays(),
                a.getUsedDays(),
                a.getPendingDays(),
                a.getRemainingDays()
        );
    }

    private LeaveManagementApi.LeaveRequestResponse toRequestResponse(LeaveRequest r, Employee emp, LeaveType t) {
        return new LeaveManagementApi.LeaveRequestResponse(
                r.getId(),
                r.getRequestNumber(),
                r.getEmployeeId(),
                emp != null ? emp.getFullName() : r.getEmployeeId(),
                r.getLeaveTypeId(),
                t != null ? t.getCode() : r.getLeaveTypeId(),
                t != null ? t.getNameAr() : r.getLeaveTypeId(),
                r.getStartDate(),
                r.getEndDate(),
                r.getTotalDays(),
                r.getStatus(),
                r.getReason(),
                r.getRejectionReason(),
                r.getApproverUserId(),
                r.getApprovedAt(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getVersion()
        );
    }
}
