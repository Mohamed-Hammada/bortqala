package com.bemo.hr.reporting.application;

import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.reporting.api.DashboardApi;
import com.bemo.hr.reporting.domain.AttendanceDecision;
import com.bemo.hr.reporting.domain.DailyStatus;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.ZoneId;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceReportRepository attendanceReportRepository;
    private final DailyAttendanceResultRepository dailyAttendanceResultRepository;
    private final ImportBatchRepository importBatchRepository;
    private final PunchRecordRepository punchRecordRepository;
    private final ZoneId companyZone;

    public DashboardService(AttendanceCategoryRepository attendanceCategoryRepository, EmployeeRepository employeeRepository,
                            AttendanceReportRepository attendanceReportRepository,
                            DailyAttendanceResultRepository dailyAttendanceResultRepository,
                            ImportBatchRepository importBatchRepository, PunchRecordRepository punchRecordRepository,
                            @Value("${hr.company-zone:Africa/Cairo}") String companyZone) {
        this.attendanceCategoryRepository = attendanceCategoryRepository; this.employeeRepository = employeeRepository;
        this.attendanceReportRepository = attendanceReportRepository; this.dailyAttendanceResultRepository = dailyAttendanceResultRepository;
        this.importBatchRepository = importBatchRepository; this.punchRecordRepository = punchRecordRepository;
        this.companyZone = ZoneId.of(companyZone);
    }

    public DashboardApi.Response dashboard(int year, int month) {
        var period = YearMonth.of(year, month);
        var report = attendanceReportRepository.findByPayCycleAndPeriodStartAndPeriodEnd(
                PayCycle.MONTHLY, period.atDay(1), period.atEndOfMonth()).orElse(null);
        var rows = report == null ? List.<com.bemo.hr.reporting.domain.DailyAttendanceResult>of()
                : dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId());
        long scheduled = rows.stream().filter(row -> row.getStatus() != DailyStatus.NON_WORKDAY && row.getStatus() != DailyStatus.HOLIDAY).count();
        long present = rows.stream().filter(row -> row.getStatus() == DailyStatus.PRESENT || row.getDecision() == AttendanceDecision.NORMAL_DAY).count();
        double rate = scheduled == 0 ? 0 : Math.round((present * 10_000.0 / scheduled)) / 100.0;
        var categoryMetrics = rows.stream().collect(java.util.stream.Collectors.groupingBy(com.bemo.hr.reporting.domain.DailyAttendanceResult::getCategoryId))
                .entrySet().stream().map(entry -> {
                    var values = entry.getValue();
                    var arrivals = values.stream().map(com.bemo.hr.reporting.domain.DailyAttendanceResult::getFirstPunch)
                            .filter(java.util.Objects::nonNull).map(value -> value.atZone(companyZone).toLocalTime().toSecondOfDay()).toList();
                    var typical = arrivals.isEmpty() ? null : java.time.LocalTime.ofSecondOfDay((long) arrivals.stream().mapToInt(Integer::intValue).average().orElse(0));
                    return new DashboardApi.CategoryMetric(entry.getKey(), values.getFirst().getCategoryName(), values.size(),
                            values.stream().filter(value -> value.getStatus() == DailyStatus.PRESENT || value.getDecision() == AttendanceDecision.NORMAL_DAY).count(),
                            values.stream().filter(com.bemo.hr.reporting.domain.DailyAttendanceResult::isBlocking).count(), typical,
                            values.stream().mapToLong(com.bemo.hr.reporting.domain.DailyAttendanceResult::getOvertimeMinutes).sum());
                }).sorted(java.util.Comparator.comparing(DashboardApi.CategoryMetric::categoryName)).toList();
        var recent = importBatchRepository.findAllByOrderByImportedAtDesc().stream().limit(5)
                .map(batch -> new DashboardApi.RecentImport(batch.getId(), batch.getFileName(), batch.getDeviceName(),
                        batch.getImportedRows(), batch.getErrorRows(), batch.getImportedAt())).toList();
        long unmatched = punchRecordRepository.summarizeUnmatched().stream()
                .filter(identity -> employeeRepository.findByDeviceUserId((String) identity[0]).isEmpty()).count();
        return new DashboardApi.Response(year, month, employeeRepository.findAll().stream().filter(e -> e.isActive()).count(),
                attendanceCategoryRepository.findAll().stream().filter(c -> c.isActive()).count(), report == null ? null : report.getStatus(),
                report == null ? null : report.getId(), report == null ? 0 : report.getUnresolvedCount(), scheduled, present, rate,
                rows.stream().filter(row -> row.getLateMinutes() > 0).count(), rows.stream().mapToLong(row -> row.getOvertimeMinutes()).sum(),
                unmatched, importBatchRepository.findAll().stream().mapToLong(batch -> batch.getImportedRows()).sum(), categoryMetrics, recent);
    }
}
