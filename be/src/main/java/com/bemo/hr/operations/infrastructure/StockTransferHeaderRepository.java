package com.bemo.hr.operations.infrastructure;

import com.bemo.hr.operations.domain.StockTransferHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferHeaderRepository extends JpaRepository<StockTransferHeader, String> {
    List<StockTransferHeader> findBySourceWarehouseIdOrTargetWarehouseId(String sourceWarehouseId, String targetWarehouseId);
}
