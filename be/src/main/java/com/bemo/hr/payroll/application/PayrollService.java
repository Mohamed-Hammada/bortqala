package com.bemo.hr.payroll.application;

import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.payroll.api.PayrollApi;
import com.bemo.hr.payroll.domain.*;
import com.bemo.hr.payroll.infrastructure.PayrollRunHeaderRepository;
import com.bemo.hr.payroll.infrastructure.PayrollRunLineRepository;
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.reporting.domain.DailyAttendanceResult;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.workforce.WorkforceAdvanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollService {

    private final SalaryPaymentRepository salaryPaymentRepository;
    private final com.bemo.hr.payroll.domain.SalaryPaymentExplanationRepository explanationRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final AttendanceReportRepository attendanceReportRepository;
    private final DailyAttendanceResultRepository dailyAttendanceResultRepository;
    private final OperationsService operationsService;
    private final WorkforceAdvanceService workforceAdvanceService;
    private final PayrollExcelExporter payrollExcelExporter;
    private final com.bemo.hr.audit.application.AuditService auditService;
    private final com.bemo.hr.reporting.application.AttendanceExceptionService attendanceExceptionService;
    private final PayrollSnapshotService payrollSnapshotService;
    private final PayrollCalculationPolicyService payrollCalculationPolicyService;
    private final PayrollRunHeaderRepository payrollRunHeaderRepository;
    private final PayrollRunLineRepository payrollRunLineRepository;
    private final PayrollGlPostingService payrollGlPostingService;
    private final PayrollPaymentAccountingService payrollPaymentAccountingService;
    private final EgyptianStatutoryPayrollService egyptianStatutoryPayrollService;

    /**
     * Maker/checker segregation of duties for payroll. Defaults to enforced; deployments that
     * operate with a single trusted operator may set {@code hr.payroll.sod-enabled=false}.
     */
    @Value("${hr.payroll.sod-enabled:true}")
    private String sodEnabled;

    private boolean sodActive() {
        return !"false".equalsIgnoreCase(sodEnabled);
    }

    private boolean isSuperAdminActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    private void assertApproverIsNotPreparer(PayrollRunHeader run, String actor) {
        if (!sodActive() || isSuperAdminActor() || actor == null) {
            return;
        }
        if (actor.equals(run.getCalculatedBy())) {
            log.warn("SoD violation blocked: preparer {} attempted to approve/post payroll run {}", actor, run.getId());
            throw new BusinessRuleException(
                    "The user who calculated this payroll cannot approve or post it.",
                    "PAYROLL_SOD_SELF_APPROVAL", HttpStatus.CONFLICT);
        }
    }

    private void assertDisburserIsNotApprover(PayrollRunHeader run, String actor) {
        if (!sodActive() || isSuperAdminActor() || actor == null) {
            return;
        }
        if (actor.equals(run.getApprovedBy())) {
            log.warn("SoD violation blocked: approver {} attempted to disburse payroll run {}", actor, run.getId());
            throw new BusinessRuleException(
                    "The user who approved or posted this payroll cannot disburse its payments.",
                    "PAYROLL_SOD_DISBURSEMENT_CONFLICT", HttpStatus.CONFLICT);
        }
    }

    public List<PayrollApi.ExplanationResponse> getPaymentExplanation(String paymentId) {
        log.debug("getPaymentExplanation called with paymentId={}", paymentId);
        var explanations = explanationRepository.findBySalaryPaymentIdOrderByCreatedAtAsc(paymentId);
        if (explanations.isEmpty()) {
            var payment = salaryPaymentRepository.findById(paymentId).orElse(null);
            if (payment != null) {
                saveDefaultExplanations(payment);
                explanations = explanationRepository.findBySalaryPaymentIdOrderByCreatedAtAsc(paymentId);
            }
        }
        return explanations.stream()
                .map(e -> new PayrollApi.ExplanationResponse(
                        e.getId(), e.getSalaryPaymentId(), e.getComponentType(),
                        e.getFormula(), e.getInputValuesJson(), e.getCalculatedAmount(),
                        e.getExplanationTextAr(), e.getExplanationTextEn(), e.getCreatedAt().toEpochMilli()
                )).toList();
    }

    private void saveDefaultExplanations(SalaryPayment payment) {
        var snapshot = payrollSnapshotService.findById(payment.getPayrollSnapshotId()).orElse(null);
        BigDecimal gross = payment.getGrossAmount() != null ? payment.getGrossAmount() : BigDecimal.ZERO;
        BigDecimal adv = payment.getAdvancesDeducted() != null ? payment.getAdvancesDeducted() : BigDecimal.ZERO;
        BigDecimal otherDed = payment.getOtherDeductions() != null ? payment.getOtherDeductions() : BigDecimal.ZERO;
        BigDecimal net = payment.getNetAmount() != null ? payment.getNetAmount() : gross.subtract(adv).subtract(otherDed).max(BigDecimal.ZERO);

        String calculationInputs = snapshot == null ? "{\"gross\":" + gross + "}"
                : "{\"snapshotId\":\"" + snapshot.getId() + "\",\"baseSalary\":" + snapshot.getBaseSalary()
                  + ",\"overtimeMinutes\":" + snapshot.getOvertimeMinutes() + ",\"lateMinutes\":"
                  + snapshot.getLateMinutes() + ",\"workingHourDivisor\":" + snapshot.getWorkingHourDivisor()
                  + ",\"overtimeMultiplier\":" + snapshot.getOvertimeMultiplier() + "}";
        explanationRepository.save(new com.bemo.hr.payroll.domain.SalaryPaymentExplanation(
                payment.getId(), "SNAPSHOT_CALCULATION", "base + overtime - lateness - advances + adjustments", calculationInputs,
                gross, "Gross base salary derived from locked attendance records", "Gross base salary derived from locked attendance records"
        ));

        // Egyptian Statutory Deductions
        if (egyptianStatutoryPayrollService != null && gross.compareTo(BigDecimal.ZERO) > 0) {
            var statutory = egyptianStatutoryPayrollService.calculate(gross);
            if (statutory.monthlyEmployeeSocialInsurance().compareTo(BigDecimal.ZERO) > 0) {
                explanationRepository.save(new com.bemo.hr.payroll.domain.SalaryPaymentExplanation(
                        payment.getId(), "STATUTORY_SOCIAL_INSURANCE",
                        "Insurable Wage × 11% (Law 148/2019)",
                        "{\"insurableWage\":" + statutory.monthlyInsurableWage() + ",\"employeeRate\":0.11,\"employerContribution\":" + statutory.monthlyEmployerSocialInsurance() + "}",
                        statutory.monthlyEmployeeSocialInsurance(),
                        "حصة الموظف في التأمينات الاجتماعية (11%) وحصة صاحب العمل (" + statutory.monthlyEmployerSocialInsurance() + " ج.م)",
                        "Employee Social Insurance (11%) and Employer Liability (" + statutory.monthlyEmployerSocialInsurance() + " EGP)"
                ));
            }
            if (statutory.monthlyIncomeTax().compareTo(BigDecimal.ZERO) > 0) {
                explanationRepository.save(new com.bemo.hr.payroll.domain.SalaryPaymentExplanation(
                        payment.getId(), "STATUTORY_INCOME_TAX",
                        "Law 30/2023 Progressive Brackets",
                        "{\"annualTaxable\":" + statutory.annualTaxableIncome() + ",\"annualTax\":" + statutory.annualIncomeTax() + ",\"monthlyTax\":" + statutory.monthlyIncomeTax() + "}",
                        statutory.monthlyIncomeTax(),
                        "ضريبة كسب العمل الشهرية وفقاً للشرائح التصاعدية بالقانون 30 لسنة 2023 بعد الإعفاء الشخصي 20 ألف ج.م",
                        "Monthly Salary Income Tax per Law 30/2023 progressive brackets after 20k EGP personal exemption"
                ));
            }
            if (statutory.monthlyMartyrsFund().compareTo(BigDecimal.ZERO) > 0) {
                explanationRepository.save(new com.bemo.hr.payroll.domain.SalaryPaymentExplanation(
                        payment.getId(), "STATUTORY_MARTYRS_FUND",
                        "Gross × 0.05%",
                        "{\"rate\":0.0005,\"deduction\":" + statutory.monthlyMartyrsFund() + "}",
                        statutory.monthlyMartyrsFund(),
                        "مساهمة صندوق تكريم شهداء وضحايا ومفقودي ومصابي العمليات الحربية والإرهابية وذوي الإعاقة (0.05%)",
                        "Contribution to Martyrs' Families and Disabilities Support Fund (0.05%)"
                ));
            }
        }

        if (adv.compareTo(BigDecimal.ZERO) > 0) {
            explanationRepository.save(new com.bemo.hr.payroll.domain.SalaryPaymentExplanation(
                    payment.getId(), "ADVANCE_DEDUCTION", "Gross - Active Advance Installment", "{\"advanceDeduction\":" + adv + "}",
                    adv, "Deduction of active monthly advance installment", "Deduction of active monthly advance installment"
            ));
        } else if (workforceAdvanceService.isManualDeductionPolicy(payment.getEmployeeId(),
                YearMonth.of(payment.getPeriodYear(), payment.getPeriodMonth()).atEndOfMonth())) {
            explanationRepository.save(new com.bemo.hr.payroll.domain.SalaryPaymentExplanation(
                    payment.getId(), "ADVANCE_DEDUCTION", "Skipped - manual deduction policy",
                    "{\"advanceDeduction\":0,\"skipped\":true,\"reason\":\"MANUAL_DEDUCTION_POLICY\"}",
                    BigDecimal.ZERO,
                    "لم يتم خصم السلفة تلقائياً: سياسة الخصم اليدوي مفعلة لهذا الموظف",
                    "No automatic advance deduction: the MANUAL deduction policy is active for this employee"
            ));
        }
        explanationRepository.save(new com.bemo.hr.payroll.domain.SalaryPaymentExplanation(
                payment.getId(), "NET_PAYABLE", "Gross - Deductions = Net Payable", "{\"netAmount\":" + net + "}",
                net, "Net payable amount after all deductions and advances", "Net payable amount after all deductions and advances"
        ));
    }

    public PayrollApi.SheetResponse getSheet(int year, int month, String categoryIdFilter) {
        log.debug("getSheet called with year={}, month={}, categoryIdFilter={}", year, month, categoryIdFilter);
        return buildSheet(year, month, categoryIdFilter, false, "system", null);
    }

    private PayrollApi.SheetResponse buildSheet(int year, int month, String categoryIdFilter,
                                                boolean freezeMissingSnapshots, String actor, String payrollRunId) {
        var employees = employeeRepository.findAllByOrderByFullNameAsc();
        var categories = attendanceCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(AttendanceCategory::getId, Function.identity()));
        var payments = salaryPaymentRepository.findByPeriodYearAndPeriodMonthOrderByCreatedAtDesc(year, month);
        var paymentMap = payments.stream().collect(Collectors.toMap(
                p -> p.getEmployeeId() + ":" + p.getPeriodKind(),
                Function.identity(),
                (a, b) -> a
        ));

        var monthObj = YearMonth.of(year, month);
        LocalDate start = monthObj.atDay(1);
        LocalDate end = monthObj.atEndOfMonth();
        var effectivePolicy = payrollCalculationPolicyService.effectivePolicy(end);

        var attendanceReport = attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                com.bemo.hr.employee.domain.PayCycle.MONTHLY, start, end).orElse(null);
        var attendanceRows = attendanceReport == null ? List.<DailyAttendanceResult>of()
                : dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(attendanceReport.getId());
        var attendanceMap = attendanceRows.stream().collect(Collectors.groupingBy(DailyAttendanceResult::getEmployeeId));

        var rows = new ArrayList<PayrollApi.PayrollRow>();
        int paidCount = 0;
        int pendingCount = 0;
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;
        BigDecimal totalAdvancesDeducted = BigDecimal.ZERO;

        String runPeriodId = year + "-" + String.format("%02d", month) + ":FULL_MONTH";
        PaymentStatus overallPeriodStatus = payrollRunHeaderRepository
                .findFirstByPeriodIdOrderByCreatedAtDesc(runPeriodId)
                .map(run -> switch (run.getStatus()) {
                    case DRAFT -> PaymentStatus.DRAFT;
                    case CALCULATED -> PaymentStatus.CALCULATED;
                    case REVIEWED -> PaymentStatus.REVIEWED;
                    case APPROVED -> PaymentStatus.APPROVED;
                    case POSTED -> PaymentStatus.POSTED;
                    case PAID -> PaymentStatus.PAID;
                    case CANCELLED -> PaymentStatus.REVERSED;
                })
                .orElse(PaymentStatus.DRAFT);

        for (var emp : employees) {
            if (!emp.isActive()) continue;
            var cat = categories.get(emp.getCategoryId());
            if (categoryIdFilter != null && !categoryIdFilter.isBlank() && !categoryIdFilter.equalsIgnoreCase(emp.getCategoryId())) {
                continue;
            }

            BigDecimal base = emp.getBaseSalary() == null ? BigDecimal.ZERO : emp.getBaseSalary();
            boolean incompleteProfile = base.signum() <= 0 && cat == null;
            if (incompleteProfile) continue; // Exclude incomplete profiles from payroll sheet

            var activeAdvances = operationsService.getAdvanceBalance(emp.getId());
            if (activeAdvances == null) activeAdvances = BigDecimal.ZERO;
            var periodKind = "FULL_MONTH";
            String periodId = year + "-" + String.format("%02d", month) + ":" + periodKind;
            var paymentKey = emp.getId() + ":" + periodKind;
            var payment = paymentMap.get(paymentKey);

            var empPunches = attendanceMap.getOrDefault(emp.getId(), List.of());
            long overtimeMins = empPunches.stream().mapToLong(DailyAttendanceResult::getOvertimeMinutes).sum();
            long lateMins = empPunches.stream().mapToLong(DailyAttendanceResult::getLateMinutes).sum();
            long workedMins = empPunches.stream().mapToLong(DailyAttendanceResult::getWorkedMinutes).sum();

            BigDecimal hourlyRate = base.signum() <= 0 ? BigDecimal.ZERO
                    : base.divide(effectivePolicy.getWorkingHourDivisor(), 8, RoundingMode.HALF_UP);
            BigDecimal attendanceBonus = hourlyRate.multiply(BigDecimal.valueOf(overtimeMins))
                    .multiply(effectivePolicy.getOvertimeMultiplier()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            BigDecimal attendanceDeduction = hourlyRate.multiply(BigDecimal.valueOf(lateMins)).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            BigDecimal gross = base.add(attendanceBonus);
            BigDecimal advances = BigDecimal.ZERO;
            BigDecimal deductions = attendanceDeduction;
            BigDecimal bonuses = attendanceBonus;
            BigDecimal net = BigDecimal.ZERO;
            PaymentStatus status = PaymentStatus.DRAFT;
            Instant paidAt = null;
            PaymentMethod method = null;
            String ref = null;
            String note = null;
            String createdBy = "system";
            Instant createdAt = Instant.now();
            String id = null;
            String reportId = attendanceReport == null ? null : attendanceReport.getId();

            if (payment != null) {
                id = payment.getId();
                reportId = payment.getReportId() == null ? reportId : payment.getReportId();
                gross = payment.getGrossAmount() == null ? gross : payment.getGrossAmount();
                advances = payment.getAdvancesDeducted() == null ? BigDecimal.ZERO : payment.getAdvancesDeducted();
                deductions = payment.getOtherDeductions() == null ? deductions : payment.getOtherDeductions();
                bonuses = payment.getBonuses() == null ? bonuses : payment.getBonuses();
                net = payment.getNetAmount() == null ? gross.subtract(advances).subtract(deductions).add(bonuses).max(BigDecimal.ZERO) : payment.getNetAmount();
                status = payment.getPaymentStatus() == null ? PaymentStatus.DRAFT : payment.getPaymentStatus();
                paidAt = payment.getPaidAt();
                method = payment.getPaymentMethod();
                ref = payment.getReferenceCode();
                note = payment.getNote();
                createdBy = payment.getCreatedBy();
                createdAt = payment.getCreatedAt();
            } else {
                // Derived immutable estimation with advance safety
                BigDecimal availableForAdvance = gross.subtract(deductions).max(BigDecimal.ZERO);
                if (workforceAdvanceService.isManualDeductionPolicy(emp.getId(), end)) {
                    // WP-07: MANUAL deduction policy blocks automatic payroll collection
                    advances = BigDecimal.ZERO;
                } else {
                    advances = workforceAdvanceService.calculateEmployeePayrollDeduction(
                            emp.getId(), end, availableForAdvance, activeAdvances);
                }
                net = gross.subtract(advances).subtract(deductions).max(BigDecimal.ZERO);
            }

            String employeeRunId = payment != null && payment.getPayrollRunId() != null
                    ? payment.getPayrollRunId() : payrollRunId;
            var snapshot = payrollSnapshotService.find(employeeRunId, emp.getId()).orElse(null);
            if (snapshot == null && freezeMissingSnapshots) {
                snapshot = payrollSnapshotService.captureSnapshot(new PayrollSnapshotService.CalculationInputs(
                        employeeRunId, emp.getId(), periodId, start, end, base, workedMins, overtimeMins, lateMins,
                        absenceDays(empPunches),
                        effectivePolicy.getId(), effectivePolicy.getVersion(), effectivePolicy.getWorkingHourDivisor(),
                        effectivePolicy.getOvertimeMultiplier(), BigDecimal.ZERO, BigDecimal.ZERO,
                        activeAdvances, advances), actor);
                final var frozen = snapshot;
                payrollRunLineRepository.findByRunIdAndEmployeeId(employeeRunId, emp.getId()).orElseGet(() ->
                        payrollRunLineRepository.save(new PayrollRunLine(employeeRunId, emp.getId(), frozen.getId(),
                                frozen.getBaseSalary(), frozen.getAllowanceAmount(),
                                frozen.getDeductionAmount().add(frozen.getAdvanceDeduction()))));
            }
            if (snapshot != null) {
                base = snapshot.getBaseSalary();
                attendanceBonus = snapshot.getAllowanceAmount();
                attendanceDeduction = snapshot.getDeductionAmount();
                activeAdvances = snapshot.getAdvanceBalance();
                if (payment == null) {
                    gross = snapshot.getGrossPay();
                    advances = snapshot.getAdvanceDeduction();
                    deductions = snapshot.getDeductionAmount();
                    bonuses = snapshot.getAllowanceAmount();
                    net = snapshot.getNetPay();
                }
            }

            if (status == PaymentStatus.PAID) {
                paidCount++;
                totalPaid = totalPaid.add(net);
            } else {
                pendingCount++;
                totalPending = totalPending.add(net);
            }

            totalGross = totalGross.add(gross);
            totalAdvancesDeducted = totalAdvancesDeducted.add(advances);

            rows.add(new PayrollApi.PayrollRow(
                    id,
                    emp.getId(),
                    emp.getEmployeeCode(),
                    emp.getFullName(),
                    emp.getCategoryId(),
                    cat == null ? "—" : cat.getName(),
                    emp.getEmploymentType() == null ? "FIXED" : emp.getEmploymentType().name(),
                    reportId,
                    year,
                    month,
                    periodKind,
                    start,
                    end,
                    base,
                    attendanceBonus,
                    attendanceDeduction,
                    activeAdvances,
                    gross,
                    advances,
                    deductions,
                    bonuses,
                    net,
                    status,
                    paidAt,
                    method,
                    ref,
                    note,
                    incompleteProfile,
                    createdBy,
                    createdAt,
                    payment == null ? null : payment.getPaidBy(),
                    payment == null ? null : payment.getReversedBy(),
                    payment == null ? null : payment.getReversedAt(),
                    payment == null ? null : payment.getReversalReason(),
                    payment == null ? 0L : payment.getVersion()
            ));
        }

        var summary = new PayrollApi.Summary(
                rows.size(),
                paidCount,
                pendingCount,
                totalGross,
                totalPaid,
                totalPending,
                totalAdvancesDeducted
        );

        return new PayrollApi.SheetResponse(year, month, overallPeriodStatus, summary, rows);
    }

    @Transactional
    public PayrollApi.SheetResponse recordPayment(PayrollApi.PaymentRequest request, String actor) {
        log.debug("recordPayment called with employeeId={}, periodYear={}, periodMonth={}, actor={}", request.employeeId(), request.periodYear(), request.periodMonth(), actor);
        var emp = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new NotFoundException("Employee not found.", "HRCFG_EMPLOYEE_NOT_FOUND"));

        if (!emp.isActive() || (emp.getBaseSalary() != null && emp.getBaseSalary().signum() <= 0 && emp.getCategoryId() == null)) {
            log.warn("Validation failed: Employee incomplete profile employeeId={}", request.employeeId());
            throw new BusinessRuleException("Employee profile is incomplete (missing base salary or category). Please complete their profile at /employees first.", "PAYROLL_EMPLOYEE_INCOMPLETE", HttpStatus.CONFLICT);
        }

        var periodKind = request.periodKind() == null || request.periodKind().isBlank() ? "FULL_MONTH" : request.periodKind();
        SalaryPayment entity = salaryPaymentRepository.findForUpdate(
                        emp.getId(), request.periodYear(), request.periodMonth(), periodKind)
                .orElseThrow(() -> new BusinessRuleException("The salary must complete approval and posting before payment.",
                        "PAYROLL_PAYMENT_STATE_INVALID", HttpStatus.CONFLICT));
        if (entity.getVersion() != request.expectedVersion()) {
            log.warn("Validation failed: Stale version for payment employeeId={}", request.employeeId());
            throw new BusinessRuleException("The salary payment was changed by another request.",
                    "PAYROLL_STALE_VERSION", HttpStatus.CONFLICT);
        }
        if (entity.getPaymentStatus() == PaymentStatus.PAID) {
            log.warn("Validation failed: Duplicate payment employeeId={}", request.employeeId());
            throw new BusinessRuleException("This salary has already been paid.",
                    "PAYROLL_DUPLICATE_PAYMENT", HttpStatus.CONFLICT);
        }
        if (entity.getPaymentStatus() != PaymentStatus.POSTED) {
            log.warn("Validation failed: Only posted salary can be paid employeeId={}", request.employeeId());
            throw new BusinessRuleException("Only a posted salary can be paid.",
                    "PAYROLL_PAYMENT_STATE_INVALID", HttpStatus.CONFLICT);
        }
        if (entity.getPayrollRunId() == null || entity.getPayrollSnapshotId() == null
                || payrollSnapshotService.findById(entity.getPayrollSnapshotId()).isEmpty()) {
            log.warn("Validation failed: Frozen payroll calculation evidence required employeeId={}", request.employeeId());
            throw new BusinessRuleException("Frozen payroll calculation evidence is required before payment.",
                    "PAYROLL_RUN_SNAPSHOTS_REQUIRED", HttpStatus.CONFLICT);
        }
        PayrollRunHeader run = payrollRunHeaderRepository.findByIdForUpdate(entity.getPayrollRunId())
                .orElseThrow(() -> new BusinessRuleException("Payroll run not found",
                        "PAYROLL_RUN_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (run.getStatus() != PayrollRunHeader.Status.POSTED) {
            log.warn("Validation failed: Payroll run must be posted before payment runId={}", entity.getPayrollRunId());
            throw new BusinessRuleException("The payroll run must be posted before payment.",
                    "PAYROLL_PAYMENT_STATE_INVALID", HttpStatus.CONFLICT);
        }
        assertDisburserIsNotApprover(run, actor);

        LocalDate pStart = entity.getPeriodStart();
        LocalDate pEnd = entity.getPeriodEnd();
        var payrollReport = attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                com.bemo.hr.employee.domain.PayCycle.MONTHLY, pStart, pEnd).orElse(null);
        attendanceExceptionService.assertPayrollReady(payrollReport == null ? null : payrollReport.getId(), emp.getId());

        Instant paidAtInstant = request.paidAtEpochMs() == null ? Instant.now() : Instant.ofEpochMilli(request.paidAtEpochMs());
        entity.markAsPaid(request.paymentMethod(), paidAtInstant, request.referenceCode(), request.note(), actor);
        var disbursementJournal = payrollPaymentAccountingService.postDisbursement(entity, actor);
        entity.attachPaymentJournal(disbursementJournal.getId());
        salaryPaymentRepository.save(entity);

        BigDecimal advances = entity.getAdvancesDeducted();
        if (advances.signum() > 0) {
            String periodReference = request.periodYear() + "/" + request.periodMonth();
            operationsService.recordAdvanceSettlement(
                    emp.getId(),
                    advances.negate(),
                    "Automatic advance settlement for payroll " + periodReference,
                    paidAtInstant,
                    actor
            );
            workforceAdvanceService.applyEmployeePayrollSettlement(emp.getId(), advances, periodReference, actor);
        }

        if (salaryPaymentRepository.findByPayrollRunId(run.getId()).stream()
                .allMatch(payment -> payment.getId().equals(entity.getId())
                        || payment.getPaymentStatus() == PaymentStatus.PAID)) {
            run.transitionTo(PayrollRunHeader.Status.PAID);
            payrollRunHeaderRepository.save(run);
        }

        auditService.record("PAYROLL_DISBURSEMENT", "SALARY_PAYMENT", entity.getId(), actor,
                "{\"employeeId\":\"" + emp.getId() + "\",\"employeeName\":\"" + emp.getFullName()
                        + "\",\"previousStatus\":\"POSTED\",\"newStatus\":\"PAID\",\"paidBy\":\""
                        + actor + "\",\"net\":" + entity.getNetAmount() + "}", null);

        log.info("SalaryPayment paid id={} employeeId={}", entity.getId(), emp.getId());
        return getSheet(request.periodYear(), request.periodMonth(), null);
    }

    @Transactional
    public PayrollApi.SheetResponse transitionStatus(PayrollApi.StatusTransitionRequest request, String actor) {
        log.debug("transitionStatus called with periodYear={}, periodMonth={}, targetStatus={}, actor={}", request.periodYear(), request.periodMonth(), request.targetStatus(), actor);
        PaymentStatus requiredCurrent = switch (request.targetStatus()) {
            case CALCULATED -> PaymentStatus.DRAFT;
            case REVIEWED -> PaymentStatus.CALCULATED;
            case APPROVED -> PaymentStatus.REVIEWED;
            case POSTED -> PaymentStatus.APPROVED;
            default -> {
                log.warn("Validation failed: Invalid target status={}", request.targetStatus());
                throw new BusinessRuleException("This payroll state requires a dedicated command.",
                        "PAYROLL_STATE_TRANSITION_INVALID", HttpStatus.CONFLICT);
            }
        };
        String periodId = request.periodYear() + "-" + String.format("%02d", request.periodMonth()) + ":FULL_MONTH";
        PayrollRunHeader run = resolveRun(periodId, YearMonth.of(request.periodYear(), request.periodMonth()).atEndOfMonth());
        if (request.targetStatus() == PaymentStatus.POSTED && run.getStatus() == PayrollRunHeader.Status.POSTED) {
            payrollGlPostingService.getGlPosting(periodId);
            return getSheet(request.periodYear(), request.periodMonth(), null);
        }
        if (request.targetStatus() == PaymentStatus.APPROVED || request.targetStatus() == PaymentStatus.POSTED) {
            assertApproverIsNotPreparer(run, actor);
        }
        boolean freezeSnapshots = request.targetStatus() == PaymentStatus.CALCULATED;
        var sheet = buildSheet(request.periodYear(), request.periodMonth(), null, freezeSnapshots, actor, run.getId());
        if (sheet.rows().isEmpty()) {
            log.warn("Validation failed: Payroll register has no payable employees periodYear={} periodMonth={}", request.periodYear(), request.periodMonth());
            throw new BusinessRuleException("The payroll register has no payable employees.",
                    "PAYROLL_REGISTER_EMPTY", HttpStatus.CONFLICT);
        }
        if (request.targetStatus() == PaymentStatus.APPROVED || request.targetStatus() == PaymentStatus.POSTED) {
            sheet.rows().stream().map(PayrollApi.PayrollRow::reportId).filter(java.util.Objects::nonNull).distinct()
                    .forEach(reportId -> attendanceExceptionService.assertPayrollReady(reportId, null));
        }
        for (var row : sheet.rows()) {
            var periodKind = row.periodKind() == null ? "FULL_MONTH" : row.periodKind();
            var existingOpt = salaryPaymentRepository.findForUpdate(
                    row.employeeId(), request.periodYear(), request.periodMonth(), periodKind);
            SalaryPayment entity = existingOpt.orElseGet(() -> new SalaryPayment(
                    row.employeeId(), row.reportId(), request.periodYear(), request.periodMonth(), periodKind,
                    row.periodStart(), row.periodEnd(), row.grossAmount(), row.advancesDeducted(),
                    row.otherDeductions(), row.bonuses(), row.netAmount(), PaymentStatus.DRAFT, null, null,
                    null, "Payroll register created", actor));
            PaymentStatus current = entity.getPaymentStatus() == PaymentStatus.PENDING
                    ? PaymentStatus.DRAFT : entity.getPaymentStatus();
            if (current != requiredCurrent) {
                throw new BusinessRuleException("Every payroll row must be " + requiredCurrent
                        + " before transition to " + request.targetStatus() + ".",
                        "PAYROLL_STATE_TRANSITION_INVALID", HttpStatus.CONFLICT);
            }
            var snapshot = payrollSnapshotService.find(run.getId(), row.employeeId())
                    .orElseThrow(() -> new BusinessRuleException("Frozen payroll calculation evidence is required.",
                            "PAYROLL_RUN_SNAPSHOTS_REQUIRED", HttpStatus.CONFLICT));
            entity.attachCalculationEvidence(run.getId(), snapshot.getId());
            entity.transitionTo(request.targetStatus());
            salaryPaymentRepository.save(entity);
        }

        if (request.targetStatus() == PaymentStatus.CALCULATED) {
            BigDecimal runDeductions = sheet.rows().stream()
                    .map(row -> row.otherDeductions().add(row.advancesDeducted()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            run.updateTotals(sheet.summary().totalGrossAmount(), runDeductions,
                    sheet.rows().stream().map(PayrollApi.PayrollRow::netAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
            run.markCalculatedBy(actor);
        } else {
            if (request.targetStatus() == PaymentStatus.POSTED) {
                payrollGlPostingService.postApprovedRun(run, actor);
            }
            run.transitionTo(PayrollRunHeader.Status.valueOf(request.targetStatus().name()));
        }
        if (request.targetStatus() == PaymentStatus.APPROVED || request.targetStatus() == PaymentStatus.POSTED) {
            run.markApprovedBy(actor);
        }
        payrollRunHeaderRepository.save(run);

        auditService.record("PAYROLL_STATUS_TRANSITION", "PAYROLL_REGISTER", request.periodYear() + "-" + request.periodMonth(), actor,
                "{\"periodYear\":" + request.periodYear() + ",\"periodMonth\":" + request.periodMonth()
                        + ",\"previousStatus\":\"" + requiredCurrent + "\",\"newStatus\":\""
                        + request.targetStatus().name() + "\",\"actor\":\"" + actor + "\"}", null);

        log.info("PayrollRegister status transitioned to {} periodYear={} periodMonth={}", request.targetStatus(), request.periodYear(), request.periodMonth());
        return getSheet(request.periodYear(), request.periodMonth(), null);
    }

    private PayrollRunHeader resolveRun(String periodId, LocalDate runDate) {
        return payrollRunHeaderRepository.findFirstByPeriodIdOrderByCreatedAtDesc(periodId)
                .map(run -> payrollRunHeaderRepository.findByIdForUpdate(run.getId()).orElseThrow())
                .orElseGet(() -> payrollRunHeaderRepository.save(new PayrollRunHeader(
                        "PAY-" + periodId.replace(':', '-'), periodId, runDate)));
    }

    private int absenceDays(List<DailyAttendanceResult> rows) {
        return (int) rows.stream().filter(row -> row.getStatus() == com.bemo.hr.reporting.domain.DailyStatus.NO_PUNCH
                && row.getDecision() != com.bemo.hr.reporting.domain.AttendanceDecision.APPROVED_LEAVE
                && row.getDecision() != com.bemo.hr.reporting.domain.AttendanceDecision.OFFICIAL_HOLIDAY).count();
    }

    @Transactional
    public PayrollApi.SheetResponse reversePayment(PayrollApi.ReversePaymentRequest request, String actor) {
        log.debug("reversePayment called with paymentId={}, actor={}", request.paymentId(), actor);
        var payment = salaryPaymentRepository.findByIdForUpdate(request.paymentId())
                .orElseThrow(() -> new NotFoundException("Salary entry not found.", "PAYROLL_ENTRY_NOT_FOUND"));

        if (payment.getVersion() != request.expectedVersion()) {
            log.warn("Validation failed: Stale version for reversal paymentId={}", request.paymentId());
            throw new BusinessRuleException("The salary payment was changed by another request.",
                    "PAYROLL_STALE_VERSION", HttpStatus.CONFLICT);
        }
        if (payment.getPaymentStatus() == PaymentStatus.REVERSED) {
            log.warn("Validation failed: Payment already reversed paymentId={}", request.paymentId());
            throw new BusinessRuleException("This entry is already reversed.", "PAYROLL_ENTRY_ALREADY_REVERSED", HttpStatus.CONFLICT);
        }
        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
            log.warn("Validation failed: Only paid salary can be reversed paymentId={}", request.paymentId());
            throw new BusinessRuleException("Only a paid salary can be reversed.",
                    "PAYROLL_REVERSAL_STATE_INVALID", HttpStatus.CONFLICT);
        }

        PayrollRunHeader run = payrollRunHeaderRepository.findByIdForUpdate(payment.getPayrollRunId())
                .orElseThrow(() -> new BusinessRuleException("Payroll run not found",
                        "PAYROLL_RUN_NOT_FOUND", HttpStatus.NOT_FOUND));
        var reversalJournal = payrollPaymentAccountingService.reverseDisbursement(
                payment, LocalDate.now(), request.reason(), actor);

        BigDecimal deductedAdvances = payment.getAdvancesDeducted();
        if (deductedAdvances != null && deductedAdvances.signum() > 0) {
            String periodReference = payment.getPeriodYear() + "/" + payment.getPeriodMonth();
            operationsService.recordAdvanceSettlement(
                    payment.getEmployeeId(),
                    deductedAdvances,
                    "Cancel and reverse advance settlement for payroll reversal " + periodReference + " (reason: " + request.reason() + ")",
                    Instant.now(),
                    actor
            );
            workforceAdvanceService.reverseEmployeePayrollSettlement(
                    payment.getEmployeeId(), deductedAdvances, periodReference, actor);
        }

        payment.markAsReversed(request.reason(), actor);
        payment.attachReversalJournal(reversalJournal.getId());
        salaryPaymentRepository.save(payment);
        run.reopenAfterPaymentReversal();
        payrollRunHeaderRepository.save(run);

        auditService.record("PAYROLL_REVERSE", "SALARY_PAYMENT", payment.getId(), actor,
                "{\"paymentId\":\"" + payment.getId() + "\",\"previousStatus\":\"PAID\","
                        + "\"newStatus\":\"REVERSED\",\"reversedBy\":\"" + actor + "\",\"reason\":\""
                        + request.reason().replace("\"", "\\\"") + "\"}", null);

        log.info("SalaryPayment reversed id={} employeeId={}", payment.getId(), payment.getEmployeeId());
        return getSheet(payment.getPeriodYear(), payment.getPeriodMonth(), null);
    }

    @Transactional
    public PayrollApi.SheetResponse payBulk(PayrollApi.BulkPaymentRequest request, String actor) {
        log.debug("payBulk called with periodYear={}, periodMonth={}, categoryId={}, actor={}", request.periodYear(), request.periodMonth(), request.categoryId(), actor);
        var sheet = getSheet(request.periodYear(), request.periodMonth(), request.categoryId());
        if (sheet.rows().isEmpty() || sheet.rows().stream().anyMatch(row -> row.incompleteProfile()
                || row.id() == null || row.paymentStatus() != PaymentStatus.POSTED)) {
            log.warn("Validation failed: Bulk payment requires every selected row to be posted and payable periodYear={} periodMonth={}", request.periodYear(), request.periodMonth());
            throw new BusinessRuleException("Bulk payment requires every selected row to be posted and payable.",
                    "PAYROLL_BULK_NOT_PAYABLE", HttpStatus.CONFLICT);
        }
        for (var row : sheet.rows()) {
            recordPayment(new PayrollApi.PaymentRequest(
                    row.employeeId(),
                    request.periodYear(),
                    request.periodMonth(),
                    row.periodKind(),
                    request.paymentMethod() == null ? PaymentMethod.CASH : request.paymentMethod(),
                    request.referenceCode(),
                    request.note(),
                    System.currentTimeMillis(),
                    row.version()
            ), actor);
        }

        auditService.record("PAYROLL_BULK_DISBURSEMENT", "PAYROLL_REGISTER", request.periodYear() + "-" + request.periodMonth(), actor,
                "{\"periodYear\":" + request.periodYear() + ",\"periodMonth\":" + request.periodMonth() + "}", null);

        log.info("Payroll bulk payment completed periodYear={} periodMonth={} employeeCount={}", request.periodYear(), request.periodMonth(), sheet.rows().size());
        return getSheet(request.periodYear(), request.periodMonth(), request.categoryId());
    }

    public byte[] export(int year, int month, String categoryId, ExcelExportOptions options) {
        log.debug("export called with year={}, month={}, categoryId={}", year, month, categoryId);
        var sheet = getSheet(year, month, categoryId);
        byte[] data = payrollExcelExporter.export(sheet, options);
        log.info("Payroll export completed year={} month={} rows={}", year, month, sheet.rows().size());
        return data;
    }
}
