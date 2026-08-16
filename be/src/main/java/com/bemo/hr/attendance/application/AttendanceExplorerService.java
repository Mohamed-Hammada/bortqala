package com.bemo.hr.attendance.application;

import com.bemo.hr.attendance.api.AttendanceExplorerApi;
import com.bemo.hr.attendance.domain.PunchRecord;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AttendanceExplorerService {
    private final PunchRecordRepository punchRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final ZoneId zoneId;

    public AttendanceExplorerService(PunchRecordRepository punchRecordRepository,
                                     EmployeeRepository employeeRepository,
                                     @Value("${hr.company-zone:${app.time-zone:Africa/Cairo}}") String timeZone) {
        this.punchRecordRepository = punchRecordRepository;
        this.employeeRepository = employeeRepository;
        this.zoneId = ZoneId.of(timeZone);
    }

    public List<AttendanceExplorerApi.MonthSummaryResponse> months() {
        List<PunchRecord> punches = punchRecordRepository.findAll();
        if (punches.isEmpty()) return List.of();

        EmployeeIndex employees = employeeIndex();
        Map<YearMonth, List<PunchRecord>> byMonth = punches.stream()
                .collect(Collectors.groupingBy(
                        punch -> YearMonth.from(punch.getPunchedAt().atZone(zoneId)),
                        TreeMap::new,
                        Collectors.toList()));

        List<AttendanceExplorerApi.MonthSummaryResponse> result = new ArrayList<>();
        byMonth.forEach((month, rows) -> {
            Map<String, List<PunchRecord>> identities = groupByIdentity(rows);
            long mapped = identities.keySet().stream().filter(id -> employees.resolve(id).isPresent()).count();
            result.add(new AttendanceExplorerApi.MonthSummaryResponse(
                    month.toString(),
                    rows.size(),
                    identities.size(),
                    mapped,
                    identities.size() - mapped,
                    rows.stream().map(PunchRecord::getPunchedAt).min(Comparator.naturalOrder()).orElseThrow().toEpochMilli(),
                    rows.stream().map(PunchRecord::getPunchedAt).max(Comparator.naturalOrder()).orElseThrow().toEpochMilli()));
        });
        result.sort(Comparator.comparing(AttendanceExplorerApi.MonthSummaryResponse::month).reversed());
        return result;
    }

    public List<AttendanceExplorerApi.EmployeeSummaryResponse> employees(String monthText) {
        YearMonth month = parseMonth(monthText);
        List<PunchRecord> punches = punchesForMonth(month);
        EmployeeIndex employees = employeeIndex();

        return groupByIdentity(punches).entrySet().stream()
                .map(entry -> toEmployeeSummary(entry.getKey(), entry.getValue(), employees.resolve(entry.getKey())))
                .sorted(Comparator
                        .comparing(AttendanceExplorerApi.EmployeeSummaryResponse::mapped).reversed()
                        .thenComparing(item -> displayName(item).toLowerCase(Locale.ROOT)))
                .toList();
    }

    public AttendanceExplorerApi.EmployeeAttendanceResponse employee(String deviceUserId, String monthText) {
        if (deviceUserId == null || deviceUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceUserId is required");
        }

        YearMonth month = monthText == null || monthText.isBlank()
                ? latestMonthFor(deviceUserId)
                : parseMonth(monthText);
        List<PunchRecord> rows = punchesForMonth(month).stream()
                .filter(punch -> deviceUserId.equals(punch.getDeviceUserId()))
                .sorted(Comparator.comparing(PunchRecord::getPunchedAt))
                .toList();
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No attendance punches found for this employee and month");
        }

        Optional<Employee> employee = employeeIndex().resolve(deviceUserId);
        String observedName = observedName(rows);
        Map<LocalDate, List<PunchRecord>> byDay = rows.stream().collect(Collectors.groupingBy(
                punch -> punch.getPunchedAt().atZone(zoneId).toLocalDate(),
                TreeMap::new,
                Collectors.toList()));

        List<AttendanceExplorerApi.AttendanceDayResponse> days = byDay.entrySet().stream()
                .map(entry -> toDay(entry.getKey(), entry.getValue()))
                .toList();
        long workedMinutes = days.stream().mapToLong(AttendanceExplorerApi.AttendanceDayResponse::workedMinutes).sum();

        return new AttendanceExplorerApi.EmployeeAttendanceResponse(
                deviceUserId,
                observedName,
                employee.map(Employee::getId).orElse(null),
                employee.map(Employee::getEmployeeCode).orElse(null),
                employee.map(Employee::getFullName).orElse(null),
                employee.isPresent(),
                month.toString(),
                rows.size(),
                rows.get(0).getPunchedAt().toEpochMilli(),
                rows.get(rows.size() - 1).getPunchedAt().toEpochMilli(),
                workedMinutes,
                days);
    }

    private AttendanceExplorerApi.EmployeeSummaryResponse toEmployeeSummary(
            String deviceUserId, List<PunchRecord> rows, Optional<Employee> employee) {
        List<PunchRecord> sorted = rows.stream().sorted(Comparator.comparing(PunchRecord::getPunchedAt)).toList();
        return new AttendanceExplorerApi.EmployeeSummaryResponse(
            deviceUserId,
            observedName(sorted),
            employee.map(Employee::getId).orElse(null),
            employee.map(Employee::getEmployeeCode).orElse(null),
            employee.map(Employee::getFullName).orElse(null),
            employee.isPresent(),
            sorted.size(),
            sorted.get(0).getPunchedAt().toEpochMilli(),
            sorted.get(sorted.size() - 1).getPunchedAt().toEpochMilli());
    }

    private AttendanceExplorerApi.AttendanceDayResponse toDay(LocalDate date, List<PunchRecord> rows) {
        List<Instant> punches = rows.stream().map(PunchRecord::getPunchedAt).sorted().toList();
        long workedMinutes = 0;
        for (int index = 0; index + 1 < punches.size(); index += 2) {
            workedMinutes += Math.max(0, Duration.between(punches.get(index), punches.get(index + 1)).toMinutes());
        }
        return new AttendanceExplorerApi.AttendanceDayResponse(
                date.toString(),
                punches.get(0).toEpochMilli(),
                punches.size() > 1 ? punches.get(punches.size() - 1).toEpochMilli() : null,
                punches.size(),
                workedMinutes,
                punches.size() % 2 != 0,
                punches.stream().map(Instant::toEpochMilli).toList());
    }

    private List<PunchRecord> punchesForMonth(YearMonth month) {
        Instant from = month.atDay(1).atStartOfDay(zoneId).toInstant();
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant();
        return punchRecordRepository.findInRange(from, to);
    }

    private YearMonth latestMonthFor(String deviceUserId) {
        return punchRecordRepository.findAll().stream()
                .filter(punch -> deviceUserId.equals(punch.getDeviceUserId()))
                .map(PunchRecord::getPunchedAt)
                .max(Comparator.naturalOrder())
                .map(instant -> YearMonth.from(instant.atZone(zoneId)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No attendance punches found for this employee"));
    }

    private Map<String, List<PunchRecord>> groupByIdentity(List<PunchRecord> punches) {
        return punches.stream()
                .filter(punch -> punch.getDeviceUserId() != null && !punch.getDeviceUserId().isBlank())
                .collect(Collectors.groupingBy(PunchRecord::getDeviceUserId, LinkedHashMap::new, Collectors.toList()));
    }

    private String observedName(List<PunchRecord> rows) {
        return rows.stream()
                .map(PunchRecord::getRawName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private EmployeeIndex employeeIndex() {
        List<Employee> employees = employeeRepository.findAllByOrderByFullNameAsc();
        Map<String, Employee> byDeviceId = employees.stream()
                .filter(item -> item.getDeviceUserId() != null && !item.getDeviceUserId().isBlank())
                .collect(Collectors.toMap(Employee::getDeviceUserId, Function.identity(), (left, right) -> left));
        Map<String, Employee> byCode = employees.stream()
                .filter(item -> item.getEmployeeCode() != null && !item.getEmployeeCode().isBlank())
                .collect(Collectors.toMap(item -> item.getEmployeeCode().toLowerCase(Locale.ROOT), Function.identity(), (left, right) -> left));
        return new EmployeeIndex(byDeviceId, byCode);
    }

    private YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must use YYYY-MM format");
        }
    }

    private static String displayName(AttendanceExplorerApi.EmployeeSummaryResponse item) {
        if (item.employeeName() != null && !item.employeeName().isBlank()) return item.employeeName();
        if (item.observedName() != null && !item.observedName().isBlank()) return item.observedName();
        return item.deviceUserId();
    }

    private record EmployeeIndex(Map<String, Employee> byDeviceId, Map<String, Employee> byCode) {
        Optional<Employee> resolve(String deviceUserId) {
            Employee employee = byDeviceId.get(deviceUserId);
            if (employee != null) return Optional.of(employee);
            return Optional.ofNullable(byCode.get(deviceUserId.toLowerCase(Locale.ROOT)));
        }
    }
}
