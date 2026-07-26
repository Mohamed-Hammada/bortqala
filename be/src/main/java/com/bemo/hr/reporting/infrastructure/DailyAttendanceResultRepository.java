package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.reporting.domain.DailyAttendanceResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyAttendanceResultRepository extends JpaRepository<DailyAttendanceResult, String> {
    List<DailyAttendanceResult> findByReportIdOrderByWorkDateAscEmployeeNameAsc(String reportId);
    List<DailyAttendanceResult> findByReportIdAndCategoryIdAndWorkDate(String reportId, String categoryId, LocalDate workDate);
    long countByReportId(String reportId);
}
