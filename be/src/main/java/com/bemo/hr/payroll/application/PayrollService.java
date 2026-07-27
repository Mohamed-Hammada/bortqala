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
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
import com.bemo.hr.reporting.domain.DailyAttendanceResult;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
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
    private final EmployeeRepository employeeRepository;
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final AttendanceReportRepository attendanceReportRepository;
    private final DailyAttendanceResultRepository dailyAttendanceResultRepository;
    private final OperationsService operationsService;
    private final PayrollExcelExporter payrollExcelExporter;

    public PayrollApi.SheetResponse getSheet(int year, int month, String categoryIdFilter) {
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
            overallPeriodStatus = payments.getFirst().getPaymentStatus();
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
            var paymentKey = emp.getId() + ":" + periodKind;
            var payment = paymentMap.get(paymentKey);

            var empPunches = attendanceMap.getOrDefault(emp.getId(), List.of());
            long overtimeMins = empPunches.stream().mapToLong(DailyAttendanceResult::getOvertimeMinutes).sum();
            long lateMins = empPunches.stream().mapToLong(DailyAttendanceResult::getLateMinutes).sum();

            BigDecimal hourlyRate = base.signum() <= 0 ? BigDecimal.ZERO : base.divide(BigDecimal.valueOf(240), 2, RoundingMode.HALF_UP);
            BigDecimal attendanceBonus = hourlyRate.multiply(BigDecimal.valueOf(overtimeMins)).multiply(BigDecimal.valueOf(1.5)).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
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
                advances = activeAdvances.signum() <= 0 ? BigDecimal.ZERO : activeAdvances.min(gross.subtract(deductions).max(BigDecimal.ZERO));
                net = gross.subtract(advances).subtract(deductions).max(BigDecimal.ZERO);
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
                .orElseThrow(() -> new NotFoundException("Employee not found."));

        if (!emp.isActive() || (emp.getBaseSalary() != null && emp.getBaseSalary().signum() <= 0 && emp.getCategoryId() == null)) {
            throw new BusinessRuleException("الموظف غير كلي البيانات (الراتب الأساسي أو الفئة). يرجى استكمال بياناته من صفحة /employees أولاً.");
        }

        var periodKind = request.periodKind() == null || request.periodKind().isBlank() ? "FULL_MONTH" : request.periodKind();
        var existingOpt = salaryPaymentRepository.findByEmployeeIdAndPeriodYearAndPeriodMonthAndPeriodKind(
                emp.getId(), request.periodYear(), request.periodMonth(), periodKind);

        if (existingOpt.isPresent()) {
            var existing = existingOpt.get();
            if (existing.getPaymentStatus() == PaymentStatus.PAID || existing.getPaymentStatus() == PaymentStatus.POSTED) {
                throw new BusinessRuleException("تم صرف وقيد راتب هذا الموظف لهذه الفترة بالفعل (منع الدفع المكرر).");
            }
            if (existing.getPaymentStatus() == PaymentStatus.APPROVED) {
                throw new BusinessRuleException("الفترة معتمدة ومقفولة ضد تعديل الرواتب.");
            }
        }

        var monthObj = YearMonth.of(request.periodYear(), request.periodMonth());
        LocalDate pStart = request.periodStart() == null ? monthObj.atDay(1) : request.periodStart();
        LocalDate pEnd = request.periodEnd() == null ? monthObj.atEndOfMonth() : request.periodEnd();

        // Immutably derive gross, deductions, advances, and net salary
        BigDecimal base = emp.getBaseSalary() == null ? BigDecimal.ZERO : emp.getBaseSalary();
        var activeAdvances = operationsService.getAdvanceBalance(emp.getId());
        if (activeAdvances == null) activeAdvances = BigDecimal.ZERO;

        BigDecimal gross = base;
        BigDecimal deductions = request.otherDeductions() == null ? BigDecimal.ZERO : request.otherDeductions();
        BigDecimal bonus = request.bonuses() == null ? BigDecimal.ZERO : request.bonuses();
        BigDecimal availableForAdvance = gross.add(bonus).subtract(deductions).max(BigDecimal.ZERO);
        BigDecimal advances = activeAdvances.signum() <= 0 ? BigDecimal.ZERO : activeAdvances.min(availableForAdvance);
        BigDecimal net = gross.add(bonus).subtract(advances).subtract(deductions).max(BigDecimal.ZERO);

        SalaryPayment entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
        } else {
            entity = new SalaryPayment(emp.getId(), null, request.periodYear(), request.periodMonth(),
                    periodKind, pStart, pEnd, gross, advances, deductions, bonus, net,
                    PaymentStatus.DRAFT, null, null, null, null, actor);
        }

        Instant paidAtInstant = request.paidAtEpochMs() == null ? Instant.now() : Instant.ofEpochMilli(request.paidAtEpochMs());
        entity.markAsPaid(gross, advances, deductions, bonus, net, request.paymentMethod(), paidAtInstant,
                request.referenceCode(), request.note(), actor);

        salaryPaymentRepository.save(entity);

        // If advances were deducted, record a settlement entry in employee advances ledger
        if (advances.signum() > 0) {
            operationsService.recordAdvanceSettlement(
                    emp.getId(),
                    advances.negate(),
                    "تسوية سلفة تلقائية مع صرف مرتب " + request.periodYear() + "/" + request.periodMonth(),
                    paidAtInstant,
                    actor
            );
        }

        return getSheet(request.periodYear(), request.periodMonth(), null);
    }

    @Transactional
    public PayrollApi.SheetResponse transitionStatus(PayrollApi.StatusTransitionRequest request, String actor) {
        var sheet = getSheet(request.periodYear(), request.periodMonth(), request.categoryId());
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
                salaryPaymentRepository.save(entity);
            }
        }
        return getSheet(request.periodYear(), request.periodMonth(), request.categoryId());
    }

    @Transactional
    public PayrollApi.SheetResponse reversePayment(PayrollApi.ReversePaymentRequest request, String actor) {
        var payment = salaryPaymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new NotFoundException("قيد الراتب غير موجود."));

        if (payment.getPaymentStatus() == PaymentStatus.REVERSED) {
            throw new BusinessRuleException("هذا القيد متراجع عنه بالفعل.");
        }

        BigDecimal deductedAdvances = payment.getAdvancesDeducted();
        if (deductedAdvances != null && deductedAdvances.signum() > 0) {
            operationsService.recordAdvanceSettlement(
                    payment.getEmployeeId(),
                    deductedAdvances,
                    "إلغاء واسترداد تسوية سلفة للتراجع عن صرف مرتب " + payment.getPeriodYear() + "/" + payment.getPeriodMonth() + " (السبب: " + request.reason() + ")",
                    Instant.now(),
                    actor
            );
        }

        payment.markAsReversed(request.reason(), actor);
        salaryPaymentRepository.save(payment);

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
        return getSheet(request.periodYear(), request.periodMonth(), request.categoryId());
    }

    public byte[] export(int year, int month, String categoryId, ExcelExportOptions options) {
        var sheet = getSheet(year, month, categoryId);
        return payrollExcelExporter.export(sheet, options);
    }
}
