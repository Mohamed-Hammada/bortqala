package com.bemo.hr.performance.infrastructure;

import com.bemo.hr.performance.domain.PerformanceCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceCycleRepository extends JpaRepository<PerformanceCycle, String> {

    List<PerformanceCycle> findAllByOrderByPeriodYearDescCreatedAtDesc();
}
