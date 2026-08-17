package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractorSettlementRepository extends JpaRepository<ContractorSettlement, String> {
    List<ContractorSettlement> findByPeriodId(String periodId);

    Optional<ContractorSettlement> findByPeriodIdAndContractorId(String periodId, String contractorId);

    List<ContractorSettlement> findByContractorId(String contractorId);
}
