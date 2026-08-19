package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.DailyWorkProgressLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyWorkProgressLineRepository extends JpaRepository<DailyWorkProgressLine, String> {

    List<DailyWorkProgressLine> findByDailyReportIdOrderByWbsCodeAsc(String dailyReportId);

    void deleteByDailyReportId(String dailyReportId);

    @Query("SELECT COALESCE(SUM(l.todayQuantity), 0) FROM DailyWorkProgressLine l " +
           "JOIN ProjectDailyReport r ON l.dailyReportId = r.id " +
           "WHERE r.projectId = :projectId AND l.wbsNodeId = :wbsNodeId AND r.status = 'APPROVED' AND r.reportDate <= :cutoffDate")
    BigDecimal sumApprovedQuantityUpToDate(
            @Param("projectId") String projectId,
            @Param("wbsNodeId") String wbsNodeId,
            @Param("cutoffDate") LocalDate cutoffDate);

    @Query("SELECT COALESCE(SUM(l.todayQuantity), 0) FROM DailyWorkProgressLine l " +
           "JOIN ProjectDailyReport r ON l.dailyReportId = r.id " +
           "WHERE r.projectId = :projectId AND l.wbsNodeId = :wbsNodeId AND r.status = 'APPROVED' AND r.reportDate < :reportDate")
    BigDecimal sumApprovedQuantityBeforeDate(
            @Param("projectId") String projectId,
            @Param("wbsNodeId") String wbsNodeId,
            @Param("reportDate") LocalDate reportDate);

    @Query("SELECT l FROM DailyWorkProgressLine l " +
           "JOIN ProjectDailyReport r ON l.dailyReportId = r.id " +
           "WHERE r.projectId = :projectId AND r.reportDate BETWEEN :startDate AND :endDate AND r.status = 'APPROVED'")
    List<DailyWorkProgressLine> findApprovedInPeriod(
            @Param("projectId") String projectId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
