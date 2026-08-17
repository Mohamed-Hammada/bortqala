package com.bemo.hr.reporting.application;

import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.payroll.domain.PaymentStatus;
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
import com.bemo.hr.reporting.api.DashboardApi;
import com.bemo.hr.reporting.domain.AttendanceDecision;
import com.bemo.hr.reporting.domain.DailyStatus;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bemo.hr.operations.OperationsService;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceReportRepository attendanceReportRepository;
    private final DailyAttendanceResultRepository dailyAttendanceResultRepository;
    private final ImportBatchRepository importBatchRepository;
    private final PunchRecordRepository punchRecordRepository;
    private final SalaryPaymentRepository salaryPaymentRepository;
    private final OperationsService operationsService;
    private final AttendanceReportRefreshService attendanceReportRefreshService;
    private final ZoneId companyZone;

    public DashboardService(AttendanceCategoryRepository attendanceCategoryRepository, EmployeeRepository employeeRepository,
                            AttendanceReportRepository attendanceReportRepository,
                            DailyAttendanceResultRepository dailyAttendanceResultRepository,
                            ImportBatchRepository importBatchRepository, PunchRecordRepository punchRecordRepository,
                            SalaryPaymentRepository salaryPaymentRepository,
                            OperationsService operationsService,
                            AttendanceReportRefreshService attendanceReportRefreshService,
                            @Value("${hr.company-zone:Africa/Cairo}") String companyZone) {
        this.attendanceCategoryRepository = attendanceCategoryRepository; this.employeeRepository = employeeRepository;
        this.attendanceReportRepository = attendanceReportRepository; this.dailyAttendanceResultRepository = dailyAttendanceResultRepository;
        this.importBatchRepository = importBatchRepository; this.punchRecordRepository = punchRecordRepository;
        this.salaryPaymentRepository = salaryPaymentRepository;
        this.operationsService = operationsService;
        this.attendanceReportRefreshService = attendanceReportRefreshService;
        this.companyZone = ZoneId.of(companyZone);
    }

    public DashboardApi.Response dashboard(int year, int month) {
        var period = YearMonth.of(year, month);
        var report = resolveAttendanceReport(period);
        var rows = report == null ? List.<com.bemo.hr.reporting.domain.DailyAttendanceResult>of()
                : dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId());
        long scheduled = rows.stream().filter(row -> row.getStatus() != DailyStatus.NON_WORKDAY && row.getStatus() != DailyStatus.HOLIDAY).count();
        long present = rows.stream().filter(row -> row.getStatus() == DailyStatus.PRESENT || row.getDecision() == AttendanceDecision.NORMAL_DAY).count();
        double rate = scheduled == 0 ? 0 : Math.round((present * 10_000.0 / scheduled)) / 100.0;
        var categoryMetrics = rows.stream().collect(Collectors.groupingBy(com.bemo.hr.reporting.domain.DailyAttendanceResult::getCategoryId))
                .entrySet().stream().map(entry -> {
                    var values = entry.getValue();
                    var arrivals = values.stream().map(com.bemo.hr.reporting.domain.DailyAttendanceResult::getFirstPunch)
                            .filter(java.util.Objects::nonNull).map(value -> value.atZone(companyZone).toLocalTime().toSecondOfDay()).toList();
                    var typical = arrivals.isEmpty() ? null : java.time.LocalTime.ofSecondOfDay((long) arrivals.stream().mapToInt(Integer::intValue).average().orElse(0));
                    return new DashboardApi.CategoryMetric(entry.getKey(), values.get(0).getCategoryName(), values.size(),
                            values.stream().filter(value -> value.getStatus() == DailyStatus.PRESENT || value.getDecision() == AttendanceDecision.NORMAL_DAY).count(),
                            values.stream().filter(com.bemo.hr.reporting.domain.DailyAttendanceResult::isBlocking).count(), typical,
                            values.stream().mapToLong(com.bemo.hr.reporting.domain.DailyAttendanceResult::getOvertimeMinutes).sum());
                }).sorted(Comparator.comparing(DashboardApi.CategoryMetric::categoryName)).toList();
        var recent = importBatchRepository.findAllByOrderByImportedAtDesc().stream().limit(5)
                .map(batch -> new DashboardApi.RecentImport(batch.getId(), batch.getFileName(), batch.getDeviceName(),
                        batch.getImportedRows(), batch.getErrorRows(), batch.getImportedAt())).toList();
        long unmatched = punchRecordRepository.summarizeUnmatched().stream()
                .filter(identity -> employeeRepository.findByDeviceUserId((String) identity[0]).isEmpty()).count();

        long singlePunchDays = rows.stream().filter(row -> row.getFirstPunch() != null && row.getLastPunch() != null && row.getFirstPunch().equals(row.getLastPunch())).count();

        return new DashboardApi.Response(
                year, month, java.time.Instant.now(),
                employeeRepository.findAll().stream().filter(com.bemo.hr.employee.domain.Employee::isActive).count(),
                attendanceCategoryRepository.findByScopeIn(java.util.List.of(
                        com.bemo.hr.employee.domain.CategoryScope.EMPLOYEE,
                        com.bemo.hr.employee.domain.CategoryScope.BOTH)).stream()
                        .filter(com.bemo.hr.employee.domain.AttendanceCategory::isActive).count(),
                report == null ? null : report.getStatus(),
                report == null ? null : report.getId(),
                report == null ? 0 : report.getUnresolvedCount(),
                scheduled, present, rate,
                rows.stream().filter(row -> row.getLateMinutes() > 0).count(),
                singlePunchDays,
                rows.stream().mapToLong(com.bemo.hr.reporting.domain.DailyAttendanceResult::getOvertimeMinutes).sum(),
                unmatched,
                importBatchRepository.findAll().stream().mapToLong(com.bemo.hr.attendance.domain.ImportBatch::getImportedRows).sum(),
                operationsService.countStockMovements(),
                operationsService.countInventoryItems(),
                operationsService.countLowStockItems(),
                operationsService.countNegativeStockItems(),
                operationsService.countPartnerLedgerEntries(),
                operationsService.countActiveParties(),
                categoryMetrics,
                recent
        );
    }

    public List<DashboardApi.AttendanceChartPoint> attendanceChart(String period, String departmentId, int year, int month) {
        var periodYm = YearMonth.of(year, month);
        var report = resolveAttendanceReport(periodYm);
        if (report == null) return List.of();

        var rows = dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId());
        if (departmentId != null && !departmentId.isBlank()) {
            rows = rows.stream().filter(r -> departmentId.equals(r.getCategoryId())).toList();
        }

        Map<LocalDate, List<com.bemo.hr.reporting.domain.DailyAttendanceResult>> byDay;
        if ("WEEK".equalsIgnoreCase(period)) {
            byDay = rows.stream().collect(Collectors.groupingBy(r -> r.getWorkDate(), LinkedHashMap::new, Collectors.toList()));
        } else {
            byDay = rows.stream().collect(Collectors.groupingBy(r -> r.getWorkDate(), LinkedHashMap::new, Collectors.toList()));
        }

        return byDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    var dayRows = entry.getValue();
                    long present = dayRows.stream().filter(r -> r.getStatus() == DailyStatus.PRESENT || r.getDecision() == AttendanceDecision.NORMAL_DAY).count();
                    long absent = dayRows.stream().filter(r -> r.getStatus() == DailyStatus.NO_PUNCH).count();
                    long late = dayRows.stream().filter(r -> r.getLateMinutes() > 0).count();
                    long exc = dayRows.stream().filter(com.bemo.hr.reporting.domain.DailyAttendanceResult::isBlocking).count();
                    return new DashboardApi.AttendanceChartPoint(entry.getKey().toString(), present, absent, late, exc);
                })
                .toList();
    }

    public DashboardApi.PayrollSummaryRecord payrollSummary(int year, int month) {
        var employees = employeeRepository.findAll().stream().filter(com.bemo.hr.employee.domain.Employee::isActive).toList();
        int totalActive = employees.size();

        var payments = salaryPaymentRepository.findByPeriodYearAndPeriodMonthOrderByCreatedAtDesc(year, month);
        int paidCount = 0;
        int pendingCount = 0;
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;

        for (var p : payments) {
            BigDecimal gross = p.getGrossAmount() == null ? BigDecimal.ZERO : p.getGrossAmount();
            BigDecimal net = p.getNetAmount() == null ? BigDecimal.ZERO : p.getNetAmount();
            totalGross = totalGross.add(gross);
            if (p.getPaymentStatus() == PaymentStatus.PAID) {
                paidCount++;
                totalPaid = totalPaid.add(net);
            } else {
                pendingCount++;
                totalPending = totalPending.add(net);
            }
        }

        return new DashboardApi.PayrollSummaryRecord(totalActive, paidCount, pendingCount, totalGross, totalPaid, totalPending);
    }

    public List<DashboardApi.DepartmentMetric> departmentMetrics(int year, int month) {
        var periodYm = YearMonth.of(year, month);
        var report = resolveAttendanceReport(periodYm);
        if (report == null) return List.of();

        var rows = dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId());
        var employees = employeeRepository.findAll().stream()
                .filter(com.bemo.hr.employee.domain.Employee::isActive)
                .collect(Collectors.groupingBy(com.bemo.hr.employee.domain.Employee::getCategoryId));

        return rows.stream()
                .collect(Collectors.groupingBy(r -> r.getCategoryId()))
                .entrySet().stream()
                .map(entry -> {
                    var catRows = entry.getValue();
                    var catName = catRows.get(0).getCategoryName();
                    long scheduled = catRows.stream().filter(r -> r.getStatus() != DailyStatus.NON_WORKDAY && r.getStatus() != DailyStatus.HOLIDAY).count();
                    long present = catRows.stream().filter(r -> r.getStatus() == DailyStatus.PRESENT || r.getDecision() == AttendanceDecision.NORMAL_DAY).count();
                    double rate = scheduled == 0 ? 0 : Math.round((present * 10_000.0 / scheduled)) / 100.0;
                    int empCount = employees.getOrDefault(entry.getKey(), List.of()).size();
                    return new DashboardApi.DepartmentMetric(entry.getKey(), catName, empCount, present, scheduled, rate);
                })
                .sorted(Comparator.comparing(DashboardApi.DepartmentMetric::departmentName))
                .toList();
    }

    public List<DashboardApi.TrendPoint> trends(int months) {

        var current = YearMonth.now(companyZone);

        return trends(months, current.getYear(), current.getMonthValue());

    }

    public List<DashboardApi.TrendPoint> trends(int months, int year, int month) {
        int capped = Math.min(Math.max(months, 1), 24);
        var anchor = YearMonth.of(year, month);
        var points = new ArrayList<DashboardApi.TrendPoint>(capped);
        for (int offset = capped - 1; offset >= 0; offset--) {
            var period = anchor.minusMonths(offset);
            points.add(pointFor(period));
        }
        return points;
    

    }

    private DashboardApi.TrendPoint pointFor(YearMonth period) {
        var report = resolveAttendanceReport(period);
        var rows = report == null ? List.<com.bemo.hr.reporting.domain.DailyAttendanceResult>of()
                : dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId());
        long scheduled = rows.stream().filter(row -> row.getStatus() != DailyStatus.NON_WORKDAY && row.getStatus() != DailyStatus.HOLIDAY).count();
        long present = rows.stream().filter(row -> row.getStatus() == DailyStatus.PRESENT || row.getDecision() == AttendanceDecision.NORMAL_DAY).count();
        double rate = scheduled == 0 ? 0 : Math.round((present * 10_000.0 / scheduled)) / 100.0;
        long exceptions = rows.stream().filter(com.bemo.hr.reporting.domain.DailyAttendanceResult::isBlocking).count();
        long overtime = rows.stream().mapToLong(com.bemo.hr.reporting.domain.DailyAttendanceResult::getOvertimeMinutes).sum();

        var payments = salaryPaymentRepository.findByPeriodYearAndPeriodMonthOrderByCreatedAtDesc(period.getYear(), period.getMonthValue());
        int paid = 0;
        int pending = 0;
        var totalGross = BigDecimal.ZERO;
        var totalPaid = BigDecimal.ZERO;
        var totalPending = BigDecimal.ZERO;
        for (var payment : payments) {
            var gross = payment.getGrossAmount() == null ? BigDecimal.ZERO : payment.getGrossAmount();
            var net = payment.getNetAmount() == null ? BigDecimal.ZERO : payment.getNetAmount();
            totalGross = totalGross.add(gross);
            if (payment.getPaymentStatus() == PaymentStatus.PAID) {
                paid++;
                totalPaid = totalPaid.add(net);
            } else {
                pending++;
                totalPending = totalPending.add(net);
            }
        }
        return new DashboardApi.TrendPoint(
                period.toString(), period.getYear(), period.getMonthValue(),
                scheduled, present, rate, exceptions, overtime,
                paid, pending, totalGross, totalPaid);
    }

    private com.bemo.hr.reporting.domain.AttendanceReport resolveAttendanceReport(YearMonth period) {
        var existing = attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                PayCycle.MONTHLY, period.atDay(1), period.atEndOfMonth());
        if (attendanceReportRefreshService.needsRefresh(period, existing.isPresent())) {
            attendanceReportRefreshService.refreshMonth(period.getYear(), period.getMonthValue(), "dashboard-auto");
            existing = attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                    PayCycle.MONTHLY, period.atDay(1), period.atEndOfMonth());
        }
        return existing.orElse(null);
    }
}

// BORTQALA_ATTENDANCE_PIPELINE_20260816_V1_TREND_SELECTED_PERIOD
