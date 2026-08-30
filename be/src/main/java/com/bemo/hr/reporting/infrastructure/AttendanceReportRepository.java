package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.reporting.domain.AttendanceReport;
import com.bemo.hr.reporting.domain.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceReportRepository extends JpaRepository<AttendanceReport, String> {
    List<AttendanceReport> findAllByOrderByPeriodStartDesc();

    Optional<AttendanceReport> findByPayCycleAndPeriodStartAndPeriodEnd(PayCycle payCycle, LocalDate start, LocalDate end);

    boolean existsByPayCycleAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
            PayCycle payCycle, LocalDate end, LocalDate start);

    List<AttendanceReport> findByPeriodStartBetween(LocalDate start, LocalDate end);

    List<AttendanceReport> findByPeriodStartBetweenAndStatusIn(LocalDate start, LocalDate end, List<ReportStatus> statuses);

    Optional<AttendanceReport> findFirstByPeriodStartAndPeriodEndAndStatusIn(LocalDate start, LocalDate end, List<ReportStatus> statuses);
}
