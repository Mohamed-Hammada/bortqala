package com.bemo.hr.analytics.infrastructure;

import com.bemo.hr.analytics.domain.ExecutiveKpiSnapshot;
import com.bemo.hr.analytics.domain.KpiCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExecutiveKpiSnapshotRepository extends JpaRepository<ExecutiveKpiSnapshot, String> {
    List<ExecutiveKpiSnapshot> findAllByOrderByCreatedAtDesc();
    List<ExecutiveKpiSnapshot> findByPeriodKeyOrderByCategoryAscKpiKeyAsc(String periodKey);
    List<ExecutiveKpiSnapshot> findByCategoryOrderBySnapshotDateDesc(KpiCategory category);

    /**
     * Backs the upsert in {@code ExecutiveAnalyticsService.recordSnapshot} and matches the DB
     * unique constraint {@code uq_exec_kpi_snapshot_app_period_cat_key} (Liquibase v461) —
     * without this, repeated/concurrent snapshot submissions for the same KPI/period silently
     * created unbounded duplicate rows (see docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md, Medium
     * Finding M-2).
     */
    Optional<ExecutiveKpiSnapshot> findByPeriodKeyAndCategoryAndKpiKey(String periodKey, KpiCategory category, String kpiKey);
}
