package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractorSettlementAdjustmentRepository extends JpaRepository<ContractorSettlementAdjustment, String> {
    List<ContractorSettlementAdjustment> findBySettlementId(String settlementId);
    void deleteBySettlementId(String settlementId);
}
