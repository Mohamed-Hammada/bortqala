package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.DailyEquipmentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyEquipmentLogRepository extends JpaRepository<DailyEquipmentLog, String> {

    List<DailyEquipmentLog> findByDailyReportId(String dailyReportId);

    void deleteByDailyReportId(String dailyReportId);

    @Query("SELECT e FROM DailyEquipmentLog e " +
           "JOIN ProjectDailyReport r ON e.dailyReportId = r.id " +
           "WHERE r.projectId = :projectId AND r.reportDate BETWEEN :startDate AND :endDate")
    List<DailyEquipmentLog> findInPeriod(
            @Param("projectId") String projectId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
