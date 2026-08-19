package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.DailyReportStatus;
import com.bemo.hr.project.domain.ProjectDailyReport;
import com.bemo.hr.project.domain.ReportShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectDailyReportRepository extends JpaRepository<ProjectDailyReport, String> {

    List<ProjectDailyReport> findByProjectIdOrderByReportDateDescShiftDesc(String projectId);

    List<ProjectDailyReport> findByProjectIdAndReportDateBetweenOrderByReportDateAscShiftAsc(
            String projectId, LocalDate startDate, LocalDate endDate);

    Optional<ProjectDailyReport> findByProjectIdAndReportDateAndShift(
            String projectId, LocalDate reportDate, ReportShift shift);

    @Query("SELECT r FROM ProjectDailyReport r WHERE r.projectId = :projectId AND r.reportDate < :targetDate ORDER BY r.reportDate DESC, r.shift DESC")
    List<ProjectDailyReport> findLatestBeforeDate(@Param("projectId") String projectId, @Param("targetDate") LocalDate targetDate);

    @Query("SELECT r FROM ProjectDailyReport r WHERE r.projectId = :projectId AND r.status = 'APPROVED' AND r.reportDate < :targetDate")
    List<ProjectDailyReport> findApprovedBeforeDate(@Param("projectId") String projectId, @Param("targetDate") LocalDate targetDate);

    long countByProjectId(String projectId);

    long countByProjectIdAndStatus(String projectId, DailyReportStatus status);
}
