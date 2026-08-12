package com.bemo.hr.operations.infrastructure;

import com.bemo.hr.operations.domain.CycleCountHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CycleCountHeaderRepository extends JpaRepository<CycleCountHeader, String> {
    List<CycleCountHeader> findByWarehouseId(String warehouseId);
    List<CycleCountHeader> findAllByOrderByCountDateDescCreatedAtDesc();
    Optional<CycleCountHeader> findByCountNumber(String countNumber);
}
