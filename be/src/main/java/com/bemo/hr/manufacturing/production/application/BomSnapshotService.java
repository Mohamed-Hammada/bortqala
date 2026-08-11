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
    public BomSnapshot captureBomSnapshot(String productionOrderId, String bomId, int bomVersion, String componentItemId, BigDecimal requiredQuantity) {
        BomSnapshot snapshot = new BomSnapshot(productionOrderId, bomId, bomVersion, componentItemId, requiredQuantity);
        return bomSnapshotRepository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public List<BomSnapshot> getSnapshotsForProductionOrder(String productionOrderId) {
        return bomSnapshotRepository.findByProductionOrderId(productionOrderId);
    }
}
