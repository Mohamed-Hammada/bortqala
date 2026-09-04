package com.bemo.hr.operations.infrastructure;

import com.bemo.hr.operations.domain.StockTransferDiscrepancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferDiscrepancyRepository extends JpaRepository<StockTransferDiscrepancy, String> {

    List<StockTransferDiscrepancy> findByTransferId(String transferId);

    List<StockTransferDiscrepancy> findByResolutionStatus(StockTransferDiscrepancy.ResolutionStatus resolutionStatus);

    List<StockTransferDiscrepancy> findAllByOrderByReportedAtDesc();
}
