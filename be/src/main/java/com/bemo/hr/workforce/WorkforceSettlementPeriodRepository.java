package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WorkforceSettlementPeriodRepository extends JpaRepository<WorkforceSettlementPeriod, String> {
    Optional<WorkforceSettlementPeriod> findByPeriodCode(String periodCode);
    List<WorkforceSettlementPeriod> findByStatus(String status);
}
