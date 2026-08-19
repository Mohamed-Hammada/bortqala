package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.DailyMaterialConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyMaterialConsumptionRepository extends JpaRepository<DailyMaterialConsumption, String> {

    List<DailyMaterialConsumption> findByDailyReportId(String dailyReportId);

    void deleteByDailyReportId(String dailyReportId);

    @Query("SELECT m FROM DailyMaterialConsumption m " +
           "JOIN ProjectDailyReport r ON m.dailyReportId = r.id " +
           "WHERE r.projectId = :projectId AND r.reportDate BETWEEN :startDate AND :endDate")
    List<DailyMaterialConsumption> findInPeriod(
            @Param("projectId") String projectId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
