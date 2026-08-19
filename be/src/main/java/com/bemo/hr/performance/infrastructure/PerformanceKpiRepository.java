package com.bemo.hr.performance.infrastructure;

import com.bemo.hr.performance.domain.PerformanceKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceKpiRepository extends JpaRepository<PerformanceKpi, String> {

    List<PerformanceKpi> findByCycleIdOrderByCodeAsc(String cycleId);

    List<PerformanceKpi> findAllByOrderByCodeAsc();
}
