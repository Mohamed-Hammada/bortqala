package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkforceDisputeRepository extends JpaRepository<WorkforceDispute, String> {
    List<WorkforceDispute> findBySettlementPeriodId(String settlementPeriodId);
    List<WorkforceDispute> findByContractorId(String contractorId);
}
