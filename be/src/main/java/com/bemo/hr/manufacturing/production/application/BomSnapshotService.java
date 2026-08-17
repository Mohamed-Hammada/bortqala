package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.BomSnapshot;
import com.bemo.hr.manufacturing.production.infrastructure.BomSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class BomSnapshotService {

    private final BomSnapshotRepository bomSnapshotRepository;

    public BomSnapshotService(BomSnapshotRepository bomSnapshotRepository) {
        this.bomSnapshotRepository = bomSnapshotRepository;
    }

    @Transactional
    public BomSnapshot captureBomSnapshot(String productionOrderId, String bomId, String bomRevision, String componentItemId,
                                          BigDecimal requiredQuantity, BigDecimal standardUnitCost) {
        log.debug("captureBomSnapshot called with productionOrderId={}, componentItemId={}", productionOrderId, componentItemId);
        return bomSnapshotRepository.findByProductionOrderIdAndComponentItemId(productionOrderId, componentItemId)
                .orElseGet(() -> {
                    BomSnapshot snapshot = bomSnapshotRepository.save(
                            new BomSnapshot(productionOrderId, bomId, bomRevision, componentItemId, requiredQuantity,
                                    standardUnitCost));
                    log.info("BomSnapshot created for productionOrderId={}, componentItemId={}", productionOrderId, componentItemId);
                    return snapshot;
                });
    }

    @Transactional(readOnly = true)
    public List<BomSnapshot> getSnapshotsForProductionOrder(String productionOrderId) {
        log.debug("getSnapshotsForProductionOrder called with productionOrderId={}", productionOrderId);
        return bomSnapshotRepository.findByProductionOrderId(productionOrderId);
    }
}
