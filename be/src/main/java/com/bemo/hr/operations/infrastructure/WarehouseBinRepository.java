package com.bemo.hr.operations.infrastructure;

import com.bemo.hr.operations.domain.WarehouseBin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseBinRepository extends JpaRepository<WarehouseBin, String> {
    List<WarehouseBin> findByWarehouseId(String warehouseId);
}
