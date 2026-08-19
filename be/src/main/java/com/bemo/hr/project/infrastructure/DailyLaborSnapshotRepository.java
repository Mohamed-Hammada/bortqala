package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.DailyLaborSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyLaborSnapshotRepository extends JpaRepository<DailyLaborSnapshot, String> {

    List<DailyLaborSnapshot> findByDailyReportId(String dailyReportId);

    void deleteByDailyReportId(String dailyReportId);

    @Query("SELECT s FROM DailyLaborSnapshot s " +
           "JOIN ProjectDailyReport r ON s.dailyReportId = r.id " +
           "WHERE r.projectId = :projectId AND r.reportDate BETWEEN :startDate AND :endDate")
    List<DailyLaborSnapshot> findInPeriod(
            @Param("projectId") String projectId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
