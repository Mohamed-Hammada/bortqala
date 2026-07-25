package com.bemo.hr.reporting.application;

import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.calendar.domain.ConfirmedHoliday;
import com.bemo.hr.calendar.infrastructure.ConfirmedHolidayRepository;
import com.bemo.hr.employee.domain.AttendanceCategory;
import com.bemo.hr.employee.domain.AttendanceMode;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.employee.domain.ScheduleRule;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.employee.infrastructure.ScheduleRuleRepository;
import com.bemo.hr.reporting.api.ReportingApi;
import com.bemo.hr.reporting.domain.AttendanceDecision;
import com.bemo.hr.reporting.domain.AttendanceReport;
import com.bemo.hr.reporting.domain.DailyAttendanceCalculator;
import com.bemo.hr.reporting.domain.DailyAttendanceResult;
import com.bemo.hr.reporting.domain.DailyStatus;
import com.bemo.hr.reporting.domain.HolidayProposal;
import com.bemo.hr.reporting.domain.HolidayProposalStatus;
import com.bemo.hr.reporting.domain.ReportStatus;
import com.bemo.hr.reporting.infrastructure.AttendanceReportRepository;
import com.bemo.hr.reporting.infrastructure.DailyAttendanceResultRepository;
import com.bemo.hr.reporting.infrastructure.HolidayProposalRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportingService {
    private final AttendanceReportRepository attendanceReportRepository;
    private final DailyAttendanceResultRepository dailyAttendanceResultRepository;
    private final HolidayProposalRepository holidayProposalRepository;
    private final ConfirmedHolidayRepository confirmedHolidayRepository;
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final ScheduleRuleRepository scheduleRuleRepository;
    private final EmployeeRepository employeeRepository;
    private final PunchRecordRepository punchRecordRepository;
    private final ReportExporter reportExporter;
    private final ZoneId companyZone;

    public ReportingService(AttendanceReportRepository attendanceReportRepository,
                            DailyAttendanceResultRepository dailyAttendanceResultRepository,
                            HolidayProposalRepository holidayProposalRepository,
                            ConfirmedHolidayRepository confirmedHolidayRepository,
                            AttendanceCategoryRepository attendanceCategoryRepository,
                            ScheduleRuleRepository scheduleRuleRepository,
                            EmployeeRepository employeeRepository,
                            PunchRecordRepository punchRecordRepository,
                            ReportExporter reportExporter,
                            @Value("${hr.company-zone:Africa/Cairo}") String companyZone) {
        this.attendanceReportRepository = attendanceReportRepository;
        this.dailyAttendanceResultRepository = dailyAttendanceResultRepository;
        this.holidayProposalRepository = holidayProposalRepository;
        this.confirmedHolidayRepository = confirmedHolidayRepository;
        this.attendanceCategoryRepository = attendanceCategoryRepository;
        this.scheduleRuleRepository = scheduleRuleRepository;
        this.employeeRepository = employeeRepository;
        this.punchRecordRepository = punchRecordRepository;
        this.reportExporter = reportExporter;
        this.companyZone = ZoneId.of(companyZone);
    }

    public List<ReportingApi.Summary> list() {
        return attendanceReportRepository.findAllByOrderByPeriodStartDesc().stream().map(this::summary).toList();
    }

    public ReportingApi.Details get(String id) {
        return details(requireReport(id));
    }

    public List<ReportingApi.PeriodOption> availablePeriods(int year) {
        if (year < 2000 || year > 2200) throw new BusinessRuleException("Year is outside the supported range.");
        var reports = attendanceReportRepository.findByPeriodStartBetween(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        var activeCategories = attendanceCategoryRepository.findAll().stream().filter(AttendanceCategory::isActive).toList();
        boolean hasMonthly = activeCategories.isEmpty()
                || activeCategories.stream().anyMatch(category -> category.getPayCycle() == PayCycle.MONTHLY);
        boolean hasHalfMonthly = activeCategories.stream()
                .anyMatch(category -> category.getPayCycle() == PayCycle.HALF_MONTHLY);
        var options = new ArrayList<ReportingApi.PeriodOption>();
        for (int monthNumber = 1; monthNumber <= 12; monthNumber++) {
            var month = YearMonth.of(year, monthNumber);
            if (hasMonthly) addPeriod(options, reports, PayCycle.MONTHLY, month,
                    ReportingApi.PeriodKind.MONTHLY, month.atDay(1), month.atEndOfMonth());
            if (hasHalfMonthly) {
                addPeriod(options, reports, PayCycle.HALF_MONTHLY, month,
                        ReportingApi.PeriodKind.FIRST_HALF, month.atDay(1), month.atDay(15));
                addPeriod(options, reports, PayCycle.HALF_MONTHLY, month,
                        ReportingApi.PeriodKind.SECOND_HALF, month.atDay(16), month.atEndOfMonth());
            }
        }
        return List.copyOf(options);
    }

    @Transactional
    public ReportingApi.Details create(ReportingApi.CreateRequest request, String actor) {
        validatePeriod(request.periodStart(), request.periodEnd());
        PayCycle payCycle = request.payCycle();
        if (attendanceReportRepository.existsByPayCycleAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                payCycle, request.periodEnd(), request.periodStart())) {
            throw new BusinessRuleException("A report for this pay cycle already overlaps the selected period.");
        }
        var categories = attendanceCategoryRepository.findAll().stream()
                .filter(AttendanceCategory::isActive)
                .filter(category -> category.getPayCycle() == payCycle)
                .collect(Collectors.toMap(AttendanceCategory::getId, Function.identity()));
        if (categories.isEmpty()) throw new BusinessRuleException("No attendance categories use this pay cycle.");
        var schedules = scheduleRuleRepository.findAll().stream()
                .filter(schedule -> categories.containsKey(schedule.getCategoryId()))
                .collect(Collectors.groupingBy(ScheduleRule::getCategoryId));
        var employees = employeeRepository.findAll().stream()
                .filter(employee -> categories.containsKey(employee.getCategoryId())).toList();
        var report = attendanceReportRepository.save(new AttendanceReport(request.periodStart(), request.periodEnd(), payCycle,
                configurationHash(categories.values(), schedules.values().stream().flatMap(List::stream).toList(), employees), actor));

        var confirmedHolidays = confirmedHolidayRepository.findByWorkDateBetween(request.periodStart(), request.periodEnd()).stream()
                .map(holiday -> holiday.getCategoryId() + "|" + holiday.getWorkDate()).collect(Collectors.toSet());
        Instant from = request.periodStart().atStartOfDay(companyZone).toInstant();
        Instant to = request.periodEnd().plusDays(1).atStartOfDay(companyZone).toInstant();
        var currentDeviceOwners = employees.stream().filter(employee -> employee.getDeviceUserId() != null)
                .collect(Collectors.toMap(Employee::getDeviceUserId, Function.identity()));
        var byId = employees.stream().collect(Collectors.toMap(Employee::getId, Function.identity()));
        var punchMap = new HashMap<String, List<Instant>>();
        for (var punch : punchRecordRepository.findInRange(from, to)) {
            var owner = currentDeviceOwners.get(punch.getDeviceUserId());
            if (owner == null && punch.getEmployeeId() != null) owner = byId.get(punch.getEmployeeId());
            if (owner == null) continue;
            LocalDate date = punch.getPunchedAt().atZone(companyZone).toLocalDate();
            punchMap.computeIfAbsent(owner.getId() + "|" + date, ignored -> new ArrayList<>()).add(punch.getPunchedAt());
        }

        var results = new ArrayList<DailyAttendanceResult>();
        for (LocalDate date = request.periodStart(); !date.isAfter(request.periodEnd()); date = date.plusDays(1)) {
            LocalDate workDate = date;
            for (var employee : employees) {
                if (!employee.activeOn(workDate)) continue;
                var category = categories.get(employee.getCategoryId());
                if (category == null) continue;
                var schedule = schedules.getOrDefault(category.getId(), List.of()).stream()
                        .filter(rule -> rule.appliesOn(workDate)).max(Comparator.comparing(ScheduleRule::getEffectiveFrom)).orElse(null);
                results.add(DailyAttendanceCalculator.calculate(report.getId(), employee, category, schedule, workDate,
                        punchMap.getOrDefault(employee.getId() + "|" + workDate, List.of()),
                        confirmedHolidays.contains(category.getId() + "|" + workDate), companyZone));
            }
        }
        dailyAttendanceResultRepository.saveAll(results);

        var proposals = createHolidayProposals(report, results, categories);
        holidayProposalRepository.saveAll(proposals);
        report.startReview(unresolved(results, proposals));
        return details(report);
    }

    @Transactional
    public ReportingApi.Details decideDaily(String reportId, String resultId, ReportingApi.DecisionRequest request, String actor) {
        var report = requireEditable(reportId);
        var result = dailyAttendanceResultRepository.findById(resultId)
                .filter(item -> item.getReportId().equals(reportId))
                .orElseThrow(() -> new NotFoundException("Daily result not found in this report."));
        if (!result.isBlocking() && result.getDecision() == null) throw new BusinessRuleException("This row does not require an HR decision.");
        Integer worked = request.workedMinutes();
        if (worked == null && request.decision() == AttendanceDecision.NORMAL_DAY &&
                (result.getStatus() == DailyStatus.MANUAL_ENTRY || result.getStatus() == DailyStatus.SINGLE_PUNCH)) worked = result.getExpectedMinutes();
        if (worked == null && request.decision() != AttendanceDecision.NORMAL_DAY) worked = 0;
        result.decide(request.decision(), worked, request.note(), actor);
        refreshUnresolved(report);
        return details(report);
    }

    @Transactional
    public ReportingApi.Details decideHoliday(String reportId, String proposalId,
                                               ReportingApi.HolidayDecisionRequest request, String actor) {
        var report = requireEditable(reportId);
        if (request.status() == HolidayProposalStatus.PENDING) throw new BusinessRuleException("Choose CONFIRMED or REJECTED.");
        var proposal = holidayProposalRepository.findById(proposalId)
                .filter(item -> item.getReportId().equals(reportId))
                .orElseThrow(() -> new NotFoundException("Holiday proposal not found in this report."));
        if (proposal.getStatus() != HolidayProposalStatus.PENDING && proposal.getStatus() != request.status()) {
            throw new BusinessRuleException("This holiday proposal has already been decided.");
        }
        proposal.decide(request.status(), request.note(), actor);
        if (request.status() == HolidayProposalStatus.CONFIRMED) {
            confirmedHolidayRepository.findByCategoryIdAndWorkDate(proposal.getCategoryId(), proposal.getWorkDate())
                    .orElseGet(() -> confirmedHolidayRepository.save(new ConfirmedHoliday(proposal.getCategoryId(), proposal.getWorkDate(),
                            request.holidayName() == null || request.holidayName().isBlank() ? "إجازة مؤكدة" : request.holidayName(), actor)));
            dailyAttendanceResultRepository.findByReportIdAndCategoryIdAndWorkDate(reportId, proposal.getCategoryId(), proposal.getWorkDate())
                    .forEach(item -> item.confirmHoliday(actor));
        }
        refreshUnresolved(report);
        return details(report);
    }

    @Transactional
    public ReportingApi.Details approve(String id, String actor) {
        var report = requireReport(id);
        if (report.getStatus() == ReportStatus.APPROVED || report.getStatus() == ReportStatus.EXPORTED) return details(report);
        refreshUnresolved(report); report.approve(actor); return details(report);
    }

    @Transactional
    public ReportingApi.Details reopen(String id) { var report = requireReport(id); report.reopen(); refreshUnresolved(report); return details(report); }

    @Transactional
    public byte[] export(String id, ExcelExportOptions options) {
        var report = requireReport(id); byte[] bytes = reportExporter.export(details(report), options); report.markExported(); return bytes;
    }

    private List<HolidayProposal> createHolidayProposals(AttendanceReport report, List<DailyAttendanceResult> results,
                                                         Map<String, AttendanceCategory> categories) {
        var grouped = results.stream().collect(Collectors.groupingBy(item -> item.getCategoryId() + "|" + item.getWorkDate()));
        var proposals = new ArrayList<HolidayProposal>();
        for (var entry : grouped.entrySet()) {
            var group = entry.getValue(); var first = group.getFirst(); var category = categories.get(first.getCategoryId());
            if (category == null || category.getAttendanceMode() != AttendanceMode.BIOMETRIC) continue;
            long noPunchCount = group.stream().filter(item -> item.getStatus() == DailyStatus.NO_PUNCH).count();
            if (!group.isEmpty() && noPunchCount * 2 >= group.size()) {
                proposals.add(new HolidayProposal(report.getId(), first.getCategoryId(), first.getCategoryName(),
                        first.getWorkDate(), group.size()));
            }
        }
        return proposals;
    }

    private ReportingApi.Details details(AttendanceReport report) {
        var results = dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId());
        var proposals = holidayProposalRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(report.getId());
        var categories = results.stream().collect(Collectors.groupingBy(DailyAttendanceResult::getCategoryId)).entrySet().stream()
                .map(entry -> categorySummary(entry.getKey(), entry.getValue())).sorted(Comparator.comparing(ReportingApi.CategorySummary::categoryName)).toList();
        return new ReportingApi.Details(summary(report), categories, results.stream().map(this::daily).toList(),
                proposals.stream().map(this::proposal).toList());
    }

    private ReportingApi.CategorySummary categorySummary(String categoryId, List<DailyAttendanceResult> items) {
        var arrivals = items.stream().map(DailyAttendanceResult::getFirstPunch).filter(java.util.Objects::nonNull)
                .map(instant -> instant.atZone(companyZone).toLocalTime().toSecondOfDay()).toList();
        LocalTime typical = arrivals.isEmpty() ? null : LocalTime.ofSecondOfDay((long) arrivals.stream().mapToInt(Integer::intValue).average().orElse(0));
        return new ReportingApi.CategorySummary(categoryId, items.getFirst().getCategoryName(), items.size(),
                items.stream().filter(item -> item.getStatus() == DailyStatus.PRESENT).count(),
                items.stream().filter(DailyAttendanceResult::isBlocking).count(), typical,
                items.stream().mapToLong(DailyAttendanceResult::getOvertimeMinutes).sum());
    }

    private void refreshUnresolved(AttendanceReport report) {
        report.updateUnresolvedCount(unresolved(dailyAttendanceResultRepository.findByReportIdOrderByWorkDateAscEmployeeNameAsc(report.getId()),
                holidayProposalRepository.findByReportIdOrderByWorkDateAscCategoryNameAsc(report.getId())));
    }

    private int unresolved(List<DailyAttendanceResult> results, List<HolidayProposal> proposals) {
        return Math.toIntExact(results.stream().filter(DailyAttendanceResult::isBlocking).count()
                + proposals.stream().filter(proposal -> proposal.getStatus() == HolidayProposalStatus.PENDING).count());
    }

    private AttendanceReport requireReport(String id) { return attendanceReportRepository.findById(id).orElseThrow(() -> new NotFoundException("Report not found.")); }
    private AttendanceReport requireEditable(String id) { var report = requireReport(id); if (report.getStatus() != ReportStatus.IN_REVIEW) throw new BusinessRuleException("Only in-review reports can be changed."); return report; }

    private ReportingApi.Summary summary(AttendanceReport report) {
        return new ReportingApi.Summary(report.getId(), report.getPeriodStart(), report.getPeriodEnd(), report.getPayCycle(), report.getStatus(),
                report.getUnresolvedCount(), report.getCreatedBy(), report.getCreatedAt(), report.getApprovedBy(),
                report.getApprovedAt(), report.getExportedAt(), report.getVersion());
    }
    private ReportingApi.DailyResult daily(DailyAttendanceResult item) {
        return new ReportingApi.DailyResult(item.getId(), item.getEmployeeId(), item.getEmployeeCode(), item.getEmployeeName(),
                item.getCategoryId(), item.getCategoryName(), item.getWorkDate(), item.getFirstPunch(), item.getLastPunch(),
                item.getPunchCount(), item.getExpectedMinutes(), item.getWorkedMinutes(), item.getManualWorkedMinutes(),
                item.getEffectiveWorkedMinutes(), item.getLateMinutes(), item.getEarlyLeaveMinutes(), item.getOvertimeMinutes(),
                item.getStatus(), item.getWarning(), item.getDecision(), item.getDecisionNote(), item.getDecidedBy(), item.getDecidedAt(), item.getRuleVersion());
    }
    private ReportingApi.HolidayProposalView proposal(HolidayProposal item) {
        return new ReportingApi.HolidayProposalView(item.getId(), item.getCategoryId(), item.getCategoryName(), item.getWorkDate(),
                item.getActiveEmployeeCount(), item.getStatus(), item.getNote(), item.getDecidedBy(), item.getDecidedAt());
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) throw new BusinessRuleException("Report end date cannot be before start date.");
        if (start.isBefore(LocalDate.of(2000, 1, 1)) || end.isAfter(LocalDate.of(2200, 12, 31))) {
            throw new BusinessRuleException("Report dates are outside the supported range.");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(start, end) > 365) {
            throw new BusinessRuleException("A report period cannot exceed 366 days.");
        }
    }

    private void addPeriod(List<ReportingApi.PeriodOption> options, List<AttendanceReport> reports,
                           PayCycle payCycle, YearMonth month,
                           ReportingApi.PeriodKind kind, LocalDate start, LocalDate end) {
        boolean overlaps = reports.stream().anyMatch(report -> report.getPayCycle() == payCycle
                && !report.getPeriodStart().isAfter(end) && !report.getPeriodEnd().isBefore(start));
        if (!overlaps) {
            options.add(new ReportingApi.PeriodOption(month.getYear(), month.getMonthValue(), kind, start, end));
        }
    }

    private String configurationHash(java.util.Collection<AttendanceCategory> categories, List<ScheduleRule> schedules, List<Employee> employees) {
        String source = categories.stream().sorted(Comparator.comparing(AttendanceCategory::getId)).map(item -> item.getId() + ':' + item.getVersion()).collect(Collectors.joining("|"))
                + schedules.stream().sorted(Comparator.comparing(ScheduleRule::getId)).map(ScheduleRule::getId).collect(Collectors.joining("|"))
                + employees.stream().sorted(Comparator.comparing(Employee::getId)).map(item -> item.getId() + ':' + item.getVersion()).collect(Collectors.joining("|"));
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
