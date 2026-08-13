package com.bemo.hr.manufacturing.production.infrastructure;

import com.bemo.hr.manufacturing.production.domain.BomSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BomSnapshotRepository extends JpaRepository<BomSnapshot, String> {
    List<BomSnapshot> findByProductionOrderId(String productionOrderId);
    Optional<BomSnapshot> findByProductionOrderIdAndComponentItemId(String productionOrderId, String componentItemId);
}
