package com.bemo.hr.operations.infrastructure;

import com.bemo.hr.operations.domain.WarehouseBin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseBinRepository extends JpaRepository<WarehouseBin, String> {
    List<WarehouseBin> findByWarehouseId(String warehouseId);
    Optional<WarehouseBin> findByWarehouseIdAndBinCodeIgnoreCase(String warehouseId, String binCode);
}
