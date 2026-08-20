package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractorSettlementLineRepository extends JpaRepository<ContractorSettlementLine, String> {
    List<ContractorSettlementLine> findBySettlementId(String settlementId);

    List<ContractorSettlementLine> findByProjectId(String projectId);

    void deleteBySettlementId(String settlementId);
}
