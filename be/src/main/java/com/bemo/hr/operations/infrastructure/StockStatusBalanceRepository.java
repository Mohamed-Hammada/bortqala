package com.bemo.hr.operations.infrastructure;

import com.bemo.hr.operations.domain.StockStatusBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockStatusBalanceRepository extends JpaRepository<StockStatusBalance, String> {
    List<StockStatusBalance> findByWarehouseIdAndItemId(String warehouseId, String itemId);
    List<StockStatusBalance> findByWarehouseId(String warehouseId);
    Optional<StockStatusBalance> findByWarehouseIdAndBinIdAndItemIdAndStatus(String warehouseId, String binId, String itemId, StockStatusBalance.Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StockStatusBalance b where b.warehouseId = :warehouseId and b.itemId = :itemId")
    List<StockStatusBalance> findByWarehouseIdAndItemIdForUpdate(@Param("warehouseId") String warehouseId,
                                                                 @Param("itemId") String itemId);
}
