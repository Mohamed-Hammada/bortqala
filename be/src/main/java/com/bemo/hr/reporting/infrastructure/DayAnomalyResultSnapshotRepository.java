package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.reporting.domain.DayAnomalyResultSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DayAnomalyResultSnapshotRepository extends JpaRepository<DayAnomalyResultSnapshot, String> {
    List<DayAnomalyResultSnapshot> findByAnomalyId(String anomalyId);
}
