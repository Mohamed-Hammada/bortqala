package com.bemo.hr.analytics.infrastructure;

import com.bemo.hr.analytics.domain.ExecutiveCockpitTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExecutiveCockpitTargetRepository extends JpaRepository<ExecutiveCockpitTarget, String> {
    Optional<ExecutiveCockpitTarget> findByPeriodKey(String periodKey);
}
