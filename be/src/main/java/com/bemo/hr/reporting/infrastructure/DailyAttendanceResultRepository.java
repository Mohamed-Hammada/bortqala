package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.reporting.domain.DailyAttendanceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyAttendanceResultRepository extends JpaRepository<DailyAttendanceResult, String> {
    List<DailyAttendanceResult> findByReportIdOrderByWorkDateAscEmployeeNameAsc(String reportId);

    List<DailyAttendanceResult> findByReportIdAndCategoryIdAndWorkDate(String reportId, String categoryId, LocalDate workDate);

    long countByReportId(String reportId);

    @Modifying
    @Query("delete from DailyAttendanceResult d where d.reportId = :reportId")
    void deleteByReportId(@Param("reportId") String reportId);

    @Modifying
    @Query("update DailyAttendanceResult d set d.employeeCode = :newCode where d.employeeCode = :oldCode")
    int normalizeEmployeeCode(@Param("oldCode") String oldCode, @Param("newCode") String newCode);
}
