package com.bemo.hr.payroll.application;

import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.payroll.api.PayrollApi;
import com.bemo.hr.payroll.domain.PaymentMethod;
import com.bemo.hr.payroll.domain.PaymentStatus;
import com.bemo.hr.payroll.domain.SalaryPayment;
import com.bemo.hr.payroll.domain.PayrollRunHeader;
import com.bemo.hr.payroll.domain.PayrollRunLine;
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
import com.bemo.hr.payroll.infrastructure.PayrollRunHeaderRepository;
import com.bemo.hr.payroll.infrastructure.PayrollRunLineRepository;
import com.bemo.hr.reporting.domain.DailyAttendanceResult;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.workforce.WorkforceAdvanceService;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public List<PayrollApi.ExplanationResponse> getPaymentExplanation(String paymentId) {
        var explanations = explanationRepository.findBySalaryPaymentIdOrderByCreatedAtAsc(paymentId);
        if (explanations.isEmpty()) {
            // Generate fallback default transparent breakdown
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
                gross, "إجمالي الراتب المستحق المستخرج من سجل الحضور والانصراف", "Gross base salary derived from locked attendance records"
        ));
        if (adv.compareTo(BigDecimal.ZERO) > 0) {
            explanationRepository.save(new com.bemo.hr.payroll.domain.SalaryPaymentExplanation(
                    payment.getId(), "ADVANCE_DEDUCTION", "Gross - Active Advance Installment", "{\"advanceDeduction\":" + adv + "}",
                    adv, "استقطاع قسط السلفة الشهرية النشطة للموظف", "Deduction of active monthly advance installment"
            ));
        }
        explanationRepository.save(new com.bemo.hr.payroll.domain.SalaryPaymentExplanation(
                payment.getId(), "NET_PAYABLE", "Gross - Deductions = Net Payable", "{\"netAmount\":" + net + "}",
                net, "صافي الراتب المستحق للصرف بعد خصم الاستقطاعات والسلف", "Net payable amount after all deductions and advances"
        ));
    }

    public PayrollApi.SheetResponse getSheet(int year, int month, String categoryIdFilter) {
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

        PaymentStatus overallPeriodStatus = PaymentStatus.DRAFT;
        if (!payments.isEmpty()) {
            overallPeriodStatus = payments.get(0).getPaymentStatus();
        }

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
                advances = workforceAdvanceService.calculateEmployeePayrollDeduction(
                        emp.getId(), end, availableForAdvance, activeAdvances);
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

            if (status == PaymentStatus.PAID || status == PaymentStatus.POSTED) {
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
                    createdAt
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
        var emp = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new NotFoundException("Employee not found.", "HRCFG_EMPLOYEE_NOT_FOUND"));

        if (!emp.isActive() || (emp.getBaseSalary() != null && emp.getBaseSalary().signum() <= 0 && emp.getCategoryId() == null)) {
            throw new BusinessRuleException("الموظف غير كلي البيانات (الراتب الأساسي أو الفئة). يرجى استكمال بياناته من صفحة /employees أولاً.", "PAYROLL_EMPLOYEE_INCOMPLETE", HttpStatus.CONFLICT);
        }

        var periodKind = request.periodKind() == null || request.periodKind().isBlank() ? "FULL_MONTH" : request.periodKind();
        var existingOpt = salaryPaymentRepository.findByEmployeeIdAndPeriodYearAndPeriodMonthAndPeriodKind(
                emp.getId(), request.periodYear(), request.periodMonth(), periodKind);

        if (existingOpt.isPresent()) {
            var existing = existingOpt.get();
            if (existing.getPaymentStatus() == PaymentStatus.PAID || existing.getPaymentStatus() == PaymentStatus.POSTED) {
                throw new BusinessRuleException("تم صرف وقيد راتب هذا الموظف لهذه الفترة بالفعل (منع الدفع المكرر).", "PAYROLL_DUPLICATE_PAYMENT", HttpStatus.CONFLICT);
            }
            if (existing.getPaymentStatus() == PaymentStatus.APPROVED) {
                throw new BusinessRuleException("الفترة معتمدة ومقفولة ضد تعديل الرواتب.", "PAYROLL_PERIOD_LOCKED", HttpStatus.CONFLICT);
            }
        }

        var monthObj = YearMonth.of(request.periodYear(), request.periodMonth());
        LocalDate pStart = request.periodStart() == null ? monthObj.atDay(1) : request.periodStart();
        LocalDate pEnd = request.periodEnd() == null ? monthObj.atEndOfMonth() : request.periodEnd();
        var payrollReport = attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                com.bemo.hr.employee.domain.PayCycle.MONTHLY, pStart, pEnd).orElse(null);
        attendanceExceptionService.assertPayrollReady(payrollReport == null ? null : payrollReport.getId(), emp.getId());

        // Freeze every calculation input before deriving or paying the result.
        BigDecimal base = emp.getBaseSalary() == null ? BigDecimal.ZERO : emp.getBaseSalary();
        var activeAdvances = operationsService.getAdvanceBalance(emp.getId());
        if (activeAdvances == null) activeAdvances = BigDecimal.ZERO;

        BigDecimal requestedDeductions = request.otherDeductions() == null ? BigDecimal.ZERO : request.otherDeductions();
        BigDecimal requestedBonus = request.bonuses() == null ? BigDecimal.ZERO : request.bonuses();
        BigDecimal availableForAdvance = base.add(requestedBonus).subtract(requestedDeductions).max(BigDecimal.ZERO);
        BigDecimal calculatedAdvance = workforceAdvanceService.calculateEmployeePayrollDeduction(
                emp.getId(), pEnd, availableForAdvance, activeAdvances);
        var attendanceRows = payrollReport == null ? List.<DailyAttendanceResult>of()
                : dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(payrollReport.getId())
                .stream().filter(row -> row.getEmployeeId().equals(emp.getId())).toList();
        long workedMinutes = attendanceRows.stream().mapToLong(DailyAttendanceResult::getWorkedMinutes).sum();
        long overtimeMinutes = attendanceRows.stream().mapToLong(DailyAttendanceResult::getOvertimeMinutes).sum();
        long lateMinutes = attendanceRows.stream().mapToLong(DailyAttendanceResult::getLateMinutes).sum();
        String periodId = request.periodYear() + "-" + String.format("%02d", request.periodMonth()) + ":" + periodKind;
        PayrollRunHeader run = resolveRun(periodId, pEnd, actor);
        var policy = payrollCalculationPolicyService.effectivePolicy(pEnd);
        var snapshot = payrollSnapshotService.captureSnapshot(new PayrollSnapshotService.CalculationInputs(
                run.getId(), emp.getId(), periodId, pStart, pEnd, base, workedMinutes, overtimeMinutes, lateMinutes,
                absenceDays(attendanceRows),
                policy.getId(), policy.getVersion(), policy.getWorkingHourDivisor(), policy.getOvertimeMultiplier(),
                requestedDeductions, requestedBonus, activeAdvances, calculatedAdvance), actor);
        payrollRunLineRepository.findByRunIdAndEmployeeId(run.getId(), emp.getId()).orElseGet(() ->
                payrollRunLineRepository.save(new PayrollRunLine(run.getId(), emp.getId(), snapshot.getId(),
                        snapshot.getBaseSalary(), snapshot.getAllowanceAmount(),
                        snapshot.getDeductionAmount().add(snapshot.getAdvanceDeduction()))));

        BigDecimal gross = snapshot.getGrossPay();
        BigDecimal advances = snapshot.getAdvanceDeduction();
        BigDecimal deductions = snapshot.getDeductionAmount();
        BigDecimal bonus = snapshot.getAllowanceAmount();
        BigDecimal net = snapshot.getNetPay();

        SalaryPayment entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
        } else {
            entity = new SalaryPayment(emp.getId(), null, request.periodYear(), request.periodMonth(),
                    periodKind, pStart, pEnd, gross, advances, deductions, bonus, net,
                    PaymentStatus.DRAFT, null, null, null, null, actor);
        }
        entity.attachCalculationEvidence(run.getId(), snapshot.getId());

        Instant paidAtInstant = request.paidAtEpochMs() == null ? Instant.now() : Instant.ofEpochMilli(request.paidAtEpochMs());
        entity.markAsPaid(gross, advances, deductions, bonus, net, request.paymentMethod(), paidAtInstant,
                request.referenceCode(), request.note(), actor);

        salaryPaymentRepository.save(entity);

        // If advances were deducted, record a settlement entry in employee advances ledger
        if (advances.signum() > 0) {
            String periodReference = request.periodYear() + "/" + request.periodMonth();
            operationsService.recordAdvanceSettlement(
                    emp.getId(),
                    advances.negate(),
                    "تسوية سلفة تلقائية مع صرف مرتب " + periodReference,
                    paidAtInstant,
                    actor
            );
            workforceAdvanceService.applyEmployeePayrollSettlement(emp.getId(), advances, periodReference, actor);
        }

        auditService.record("PAYROLL_DISBURSEMENT", "SALARY_PAYMENT", entity.getId(), actor,
                "{\"employeeId\":\"" + emp.getId() + "\",\"employeeName\":\"" + emp.getFullName() + "\",\"net\":" + net + "}", null);

        return getSheet(request.periodYear(), request.periodMonth(), null);
    }

    @Transactional
    public PayrollApi.SheetResponse transitionStatus(PayrollApi.StatusTransitionRequest request, String actor) {
        boolean freezesCalculation = request.targetStatus() == PaymentStatus.CALCULATED
                || request.targetStatus() == PaymentStatus.REVIEWED
                || request.targetStatus() == PaymentStatus.APPROVED
                || request.targetStatus() == PaymentStatus.POSTED
                || request.targetStatus() == PaymentStatus.PAID;
        String periodId = request.periodYear() + "-" + String.format("%02d", request.periodMonth()) + ":FULL_MONTH";
        PayrollRunHeader run = freezesCalculation
                ? resolveRun(periodId, YearMonth.of(request.periodYear(), request.periodMonth()).atEndOfMonth(), actor)
                : null;
        var sheet = buildSheet(request.periodYear(), request.periodMonth(), request.categoryId(), freezesCalculation,
                actor, run == null ? null : run.getId());
        if (request.targetStatus() == PaymentStatus.APPROVED || request.targetStatus() == PaymentStatus.POSTED
                || request.targetStatus() == PaymentStatus.PAID) {
            sheet.rows().stream().map(PayrollApi.PayrollRow::reportId).filter(java.util.Objects::nonNull).distinct()
                    .forEach(reportId -> attendanceExceptionService.assertPayrollReady(reportId, null));
        }
        for (var row : sheet.rows()) {
            if (row.paymentStatus() != PaymentStatus.REVERSED && row.paymentStatus() != PaymentStatus.PAID) {
                var periodKind = row.periodKind() == null ? "FULL_MONTH" : row.periodKind();
                var existingOpt = salaryPaymentRepository.findByEmployeeIdAndPeriodYearAndPeriodMonthAndPeriodKind(
                        row.employeeId(), request.periodYear(), request.periodMonth(), periodKind);

                SalaryPayment entity;
                if (existingOpt.isPresent()) {
                    entity = existingOpt.get();
                    entity.updateStatus(request.targetStatus());
                } else {
                    entity = new SalaryPayment(row.employeeId(), row.reportId(), request.periodYear(), request.periodMonth(),
                            periodKind, row.periodStart(), row.periodEnd(), row.grossAmount(), row.advancesDeducted(),
                            row.otherDeductions(), row.bonuses(), row.netAmount(), request.targetStatus(), null, null,
                            null, "تغيير حالة كشف المرتبات إلى " + request.targetStatus().name(), actor);
                }
                if (run != null) {
                    var snapshot = payrollSnapshotService.find(run.getId(), row.employeeId()).orElseThrow();
                    entity.attachCalculationEvidence(run.getId(), snapshot.getId());
                }
                salaryPaymentRepository.save(entity);
            }
        }

        if (run != null) {
            if (run.getStatus() == PayrollRunHeader.Status.DRAFT) {
                BigDecimal runDeductions = sheet.rows().stream()
                        .map(row -> row.otherDeductions().add(row.advancesDeducted()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                run.updateTotals(sheet.summary().totalGrossAmount(), runDeductions,
                        sheet.rows().stream().map(PayrollApi.PayrollRow::netAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
            }
            if ((request.targetStatus() == PaymentStatus.APPROVED
                    || request.targetStatus() == PaymentStatus.POSTED
                    || request.targetStatus() == PaymentStatus.PAID)
                    && run.getStatus() == PayrollRunHeader.Status.CALCULATED) {
                run.approve();
            }
            if ((request.targetStatus() == PaymentStatus.POSTED || request.targetStatus() == PaymentStatus.PAID)
                    && run.getStatus() == PayrollRunHeader.Status.APPROVED) {
                run.post();
            }
            payrollRunHeaderRepository.save(run);
        }

        auditService.record("PAYROLL_STATUS_TRANSITION", "PAYROLL_REGISTER", request.periodYear() + "-" + request.periodMonth(), actor,
                "{\"periodYear\":" + request.periodYear() + ",\"periodMonth\":" + request.periodMonth() + ",\"targetStatus\":\"" + request.targetStatus().name() + "\"}", null);

        return getSheet(request.periodYear(), request.periodMonth(), request.categoryId());
    }

    private PayrollRunHeader resolveRun(String periodId, LocalDate runDate, String actor) {
        return payrollRunHeaderRepository.findFirstByPeriodIdOrderByCreatedAtDesc(periodId)
                .filter(run -> run.getStatus() == PayrollRunHeader.Status.DRAFT
                        || run.getStatus() == PayrollRunHeader.Status.CALCULATED)
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
        var payment = salaryPaymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new NotFoundException("قيد الراتب غير موجود.", "PAYROLL_ENTRY_NOT_FOUND"));

        if (payment.getPaymentStatus() == PaymentStatus.REVERSED) {
            throw new BusinessRuleException("هذا القيد متراجع عنه بالفعل.", "PAYROLL_ENTRY_ALREADY_REVERSED", HttpStatus.CONFLICT);
        }

        BigDecimal deductedAdvances = payment.getAdvancesDeducted();
        if (deductedAdvances != null && deductedAdvances.signum() > 0) {
            String periodReference = payment.getPeriodYear() + "/" + payment.getPeriodMonth();
            operationsService.recordAdvanceSettlement(
                    payment.getEmployeeId(),
                    deductedAdvances,
                    "إلغاء واسترداد تسوية سلفة للتراجع عن صرف مرتب " + periodReference + " (السبب: " + request.reason() + ")",
                    Instant.now(),
                    actor
            );
            workforceAdvanceService.reverseEmployeePayrollSettlement(
                    payment.getEmployeeId(), deductedAdvances, periodReference, actor);
        }

        payment.markAsReversed(request.reason(), actor);
        salaryPaymentRepository.save(payment);

        auditService.record("PAYROLL_REVERSE", "SALARY_PAYMENT", payment.getId(), actor,
                "{\"paymentId\":\"" + payment.getId() + "\",\"reason\":\"" + request.reason().replace("\"", "\\\"") + "\"}", null);

        return getSheet(payment.getPeriodYear(), payment.getPeriodMonth(), null);
    }

    @Transactional
    public PayrollApi.SheetResponse payBulk(PayrollApi.BulkPaymentRequest request, String actor) {
        var sheet = getSheet(request.periodYear(), request.periodMonth(), request.categoryId());
        for (var row : sheet.rows()) {
            if (row.paymentStatus() != PaymentStatus.PAID && row.paymentStatus() != PaymentStatus.POSTED && !row.incompleteProfile()) {
                recordPayment(new PayrollApi.PaymentRequest(
                        row.employeeId(),
                        request.periodYear(),
                        request.periodMonth(),
                        row.periodKind(),
                        row.periodStart(),
                        row.periodEnd(),
                        row.grossAmount(),
                        row.advancesDeducted(),
                        row.otherDeductions(),
                        row.bonuses(),
                        row.netAmount(),
                        request.paymentMethod() == null ? PaymentMethod.CASH : request.paymentMethod(),
                        request.referenceCode(),
                        request.note(),
                        System.currentTimeMillis()
                ), actor);
            }
        }

        auditService.record("PAYROLL_BULK_DISBURSEMENT", "PAYROLL_REGISTER", request.periodYear() + "-" + request.periodMonth(), actor,
                "{\"periodYear\":" + request.periodYear() + ",\"periodMonth\":" + request.periodMonth() + "}", null);

        return getSheet(request.periodYear(), request.periodMonth(), request.categoryId());
    }

    public byte[] export(int year, int month, String categoryId, ExcelExportOptions options) {
        var sheet = getSheet(year, month, categoryId);
        return payrollExcelExporter.export(sheet, options);
    }
}
