package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.BomSnapshot;
import com.bemo.hr.manufacturing.production.infrastructure.BomSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BomSnapshotService {

    private final BomSnapshotRepository bomSnapshotRepository;

    public BomSnapshotService(BomSnapshotRepository bomSnapshotRepository) {
        this.bomSnapshotRepository = bomSnapshotRepository;
    }

    @Transactional
    public BomSnapshot captureBomSnapshot(String productionOrderId, String bomId, String bomRevision, String componentItemId,
                                          BigDecimal requiredQuantity, BigDecimal standardUnitCost) {
        return bomSnapshotRepository.findByProductionOrderIdAndComponentItemId(productionOrderId, componentItemId)
                .orElseGet(() -> bomSnapshotRepository.save(
                        new BomSnapshot(productionOrderId, bomId, bomRevision, componentItemId, requiredQuantity,
                                standardUnitCost)));
    }

    @Transactional(readOnly = true)
    public List<BomSnapshot> getSnapshotsForProductionOrder(String productionOrderId) {
        return bomSnapshotRepository.findByProductionOrderId(productionOrderId);
    }
}
