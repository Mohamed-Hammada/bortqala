package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.ProductionVarianceClose;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionOrderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionVarianceCloseRepository;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class ManufacturingVarianceCloseService {

    private final ProductionVarianceCloseRepository repository;
    private final ProductionOrderRepository productionOrderRepository;
    private final BomSnapshotService bomSnapshotService;
    private final OperationsService operationsService;

    public ManufacturingVarianceCloseService(ProductionVarianceCloseRepository repository,
                                             ProductionOrderRepository productionOrderRepository,
                                             BomSnapshotService bomSnapshotService,
                                             OperationsService operationsService) {
        this.repository = repository;
        this.productionOrderRepository = productionOrderRepository;
        this.bomSnapshotService = bomSnapshotService;
        this.operationsService = operationsService;
    }

    @Transactional
    public ProductionVarianceClose calculateAndCloseVariance(String workOrderId) {
        log.debug("calculateAndCloseVariance called with workOrderId={}", workOrderId);
        var order = productionOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new BusinessRuleException("Production order not found", "MFG_PRODUCTION_ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));
        var requirements = bomSnapshotService.getSnapshotsForProductionOrder(workOrderId);
        if (requirements.isEmpty()) {
            throw new BusinessRuleException("The production order has no frozen BOM requirements.",
                    "MFG_BOM_SNAPSHOT_REQUIRED", HttpStatus.CONFLICT);
        }
        BigDecimal standardCost = requirements.stream()
                .map(requirement -> requirement.getRequiredQuantity().multiply(requirement.getStandardUnitCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualCost = requirements.stream()
                .map(requirement -> operationsService.productionIssueCost(order.getOrderNumber(), requirement.getComponentItemId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ProductionVarianceClose close = repository.findByWorkOrderId(workOrderId)
                .orElseGet(() -> new ProductionVarianceClose(workOrderId, standardCost, actualCost));
        ProductionVarianceClose saved = repository.save(close);
        log.info("ProductionVarianceClose saved for workOrderId={}", workOrderId);
        return saved;
    }

    @Transactional(readOnly = true)
    public ProductionVarianceClose getVarianceClose(String workOrderId) {
        log.debug("getVarianceClose called with workOrderId={}", workOrderId);
        return repository.findByWorkOrderId(workOrderId)
                .orElseThrow(() -> new BusinessRuleException("Production variance close record not found", "VARIANCE_CLOSE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
