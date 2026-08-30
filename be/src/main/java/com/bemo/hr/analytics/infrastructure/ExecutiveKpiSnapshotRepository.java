package com.bemo.hr.analytics.infrastructure;

import com.bemo.hr.analytics.domain.ExecutiveKpiSnapshot;
import com.bemo.hr.analytics.domain.KpiCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutiveKpiSnapshotRepository extends JpaRepository<ExecutiveKpiSnapshot, String> {
    List<ExecutiveKpiSnapshot> findAllByOrderByCreatedAtDesc();
    List<ExecutiveKpiSnapshot> findByPeriodKeyOrderByCategoryAscKpiKeyAsc(String periodKey);
    List<ExecutiveKpiSnapshot> findByCategoryOrderBySnapshotDateDesc(KpiCategory category);
}
