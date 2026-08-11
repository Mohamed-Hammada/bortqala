package com.bemo.hr.operations.infrastructure;

import com.bemo.hr.operations.domain.StockStatusBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockStatusBalanceRepository extends JpaRepository<StockStatusBalance, String> {
    List<StockStatusBalance> findByWarehouseIdAndItemId(String warehouseId, String itemId);
    Optional<StockStatusBalance> findByWarehouseIdAndBinIdAndItemIdAndStatus(String warehouseId, String binId, String itemId, StockStatusBalance.Status status);
}
