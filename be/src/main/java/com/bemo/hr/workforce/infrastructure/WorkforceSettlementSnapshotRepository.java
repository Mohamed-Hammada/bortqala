package com.bemo.hr.workforce.infrastructure;

import com.bemo.hr.workforce.domain.WorkforceSettlementSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkforceSettlementSnapshotRepository extends JpaRepository<WorkforceSettlementSnapshot, String> {
    Optional<WorkforceSettlementSnapshot> findByContractorIdAndPeriodId(String contractorId, String periodId);
}
