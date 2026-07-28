package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkerSettlementRepository extends JpaRepository<WorkerSettlement, String> {
    List<WorkerSettlement> findByPeriodId(String periodId);
    List<WorkerSettlement> findByPeriodIdAndContractorId(String periodId, String contractorId);
}
