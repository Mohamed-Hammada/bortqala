package com.bemo.hr.trade.sales.application;

import com.bemo.hr.trade.sales.domain.SalesPricingSnapshot;
import com.bemo.hr.trade.sales.infrastructure.SalesPricingSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class SalesPricingSnapshotService {

    private final SalesPricingSnapshotRepository snapshotRepository;

    public SalesPricingSnapshotService(SalesPricingSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public SalesPricingSnapshot freezePricingSnapshot(String salesOrderId, String itemId, BigDecimal unitPrice, BigDecimal discountRate) {
        log.debug("freezePricingSnapshot called with salesOrderId={}, itemId={}, unitPrice={}, discountRate={}", salesOrderId, itemId, unitPrice, discountRate);
        BigDecimal discountFactor = BigDecimal.ONE.subtract(discountRate.divide(new BigDecimal("100")));
        BigDecimal netPrice = unitPrice.multiply(discountFactor);

        SalesPricingSnapshot snapshot = new SalesPricingSnapshot(salesOrderId, itemId, unitPrice, discountRate, netPrice);
        SalesPricingSnapshot saved = snapshotRepository.save(snapshot);
        log.info("SalesPricingSnapshot {} frozen for salesOrder {} item {} successfully", saved.getId(), salesOrderId, itemId);
        return saved;
    }
}
