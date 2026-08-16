package com.bemo.hr.attendance.application;

import com.bemo.hr.attendance.api.AttendanceExplorerApi;
import com.bemo.hr.attendance.domain.PunchRecord;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceExplorerServiceTests {

    @Mock
    private PunchRecordRepository punchRecordRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private AttendanceExplorerService service;

    private final ZoneId zoneId = ZoneId.of("Africa/Cairo");

    @BeforeEach
    void setUp() {
        service = new AttendanceExplorerService(punchRecordRepository, employeeRepository, "Africa/Cairo");
    }

    @Test
    void months_returnsEmptyListWhenNoPunches() {
        when(punchRecordRepository.findAll()).thenReturn(List.of());

        List<AttendanceExplorerApi.MonthSummaryResponse> result = service.months();

        assertThat(result).isEmpty();
    }

    @Test
    void months_aggregatesPunchesAndMappedEmployeesCorrectly() {
        Instant t1 = LocalDate.of(2026, 8, 1).atTime(9, 0).atZone(zoneId).toInstant();
        Instant t2 = LocalDate.of(2026, 8, 1).atTime(17, 0).atZone(zoneId).toInstant();

        PunchRecord p1 = new PunchRecord("b1", "d1", "s1", null, "DEV-01", "Raw User", t1, "line1", 1);
        PunchRecord p2 = new PunchRecord("b1", "d1", "s1", null, "DEV-02", "Unmapped User", t2, "line2", 2);

        Employee e1 = new Employee("EMP-001", "Ahmed Ali", "DEV-01", "cat-1", EmploymentType.FIXED, LocalDate.of(2026, 1, 1), null, true);

        when(punchRecordRepository.findAll()).thenReturn(List.of(p1, p2));
        when(employeeRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(e1));

        List<AttendanceExplorerApi.MonthSummaryResponse> result = service.months();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).month()).isEqualTo("2026-08");
        assertThat(result.get(0).punchCount()).isEqualTo(2);
        assertThat(result.get(0).employeeCount()).isEqualTo(2);
        assertThat(result.get(0).mappedEmployeeCount()).isEqualTo(1);
        assertThat(result.get(0).unmatchedEmployeeCount()).isEqualTo(1);
    }

    @Test
    void employees_returnsSummariesForMonth() {
        Instant t1 = LocalDate.of(2026, 8, 1).atTime(8, 0).atZone(zoneId).toInstant();
        Instant t2 = LocalDate.of(2026, 8, 1).atTime(16, 0).atZone(zoneId).toInstant();

        PunchRecord p1 = new PunchRecord("b1", "d1", "s1", null, "DEV-01", "Ahmed Observed", t1, "line1", 1);
        PunchRecord p2 = new PunchRecord("b1", "d1", "s1", null, "DEV-01", "Ahmed Observed", t2, "line2", 2);

        Employee e1 = new Employee("EMP-001", "Ahmed Ali", "DEV-01", "cat-1", EmploymentType.FIXED, LocalDate.of(2026, 1, 1), null, true);

        when(punchRecordRepository.findInRange(any(), any())).thenReturn(List.of(p1, p2));
        when(employeeRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(e1));

        List<AttendanceExplorerApi.EmployeeSummaryResponse> result = service.employees("2026-08");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).deviceUserId()).isEqualTo("DEV-01");
        assertThat(result.get(0).employeeName()).isEqualTo("Ahmed Ali");
        assertThat(result.get(0).mapped()).isTrue();
        assertThat(result.get(0).punchCount()).isEqualTo(2);
    }

    @Test
    void employee_returnsDailyAttendanceAndCalculatesWorkedMinutes() {
        Instant punchIn = LocalDate.of(2026, 8, 1).atTime(8, 0).atZone(zoneId).toInstant();
        Instant punchOut = LocalDate.of(2026, 8, 1).atTime(16, 30).atZone(zoneId).toInstant();

        PunchRecord p1 = new PunchRecord("b1", "d1", "s1", null, "DEV-01", "Ahmed Observed", punchIn, "line1", 1);
        PunchRecord p2 = new PunchRecord("b1", "d1", "s1", null, "DEV-01", "Ahmed Observed", punchOut, "line2", 2);

        Employee e1 = new Employee("EMP-001", "Ahmed Ali", "DEV-01", "cat-1", EmploymentType.FIXED, LocalDate.of(2026, 1, 1), null, true);

        when(punchRecordRepository.findInRange(any(), any())).thenReturn(List.of(p1, p2));
        when(employeeRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(e1));

        AttendanceExplorerApi.EmployeeAttendanceResponse response = service.employee("DEV-01", "2026-08");

        assertThat(response.deviceUserId()).isEqualTo("DEV-01");
        assertThat(response.employeeName()).isEqualTo("Ahmed Ali");
        assertThat(response.punchCount()).isEqualTo(2);
        assertThat(response.workedMinutes()).isEqualTo(510); // 8h 30m = 510m
        assertThat(response.days()).hasSize(1);
        assertThat(response.days().get(0).incomplete()).isFalse();
    }

    @Test
    void employee_throwsNotFoundWhenNoPunchesForMonth() {
        when(punchRecordRepository.findInRange(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.employee("DEV-01", "2026-08"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No attendance punches found");
    }
}
