package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.reporting.domain.DayAnomalyResultSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DayAnomalyResultSnapshotRepository extends JpaRepository<DayAnomalyResultSnapshot, String> {
    List<DayAnomalyResultSnapshot> findByAnomalyId(String anomalyId);

    @Modifying
    @Query("delete from DayAnomalyResultSnapshot s where s.anomalyId in (select a.id from DayAnomaly a where a.reportId = :reportId)")
    void deleteByReportId(@Param("reportId") String reportId);
}
