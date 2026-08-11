package com.bemo.hr.trade.sales.application;

import com.bemo.hr.trade.sales.domain.SalesPricingSnapshot;
import com.bemo.hr.trade.sales.infrastructure.SalesPricingSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class SalesPricingSnapshotService {

    private final SalesPricingSnapshotRepository snapshotRepository;

    public SalesPricingSnapshotService(SalesPricingSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public SalesPricingSnapshot freezePricingSnapshot(String salesOrderId, String itemId, BigDecimal unitPrice, BigDecimal discountRate) {
        BigDecimal discountFactor = BigDecimal.ONE.subtract(discountRate.divide(new BigDecimal("100")));
        BigDecimal netPrice = unitPrice.multiply(discountFactor);

        SalesPricingSnapshot snapshot = new SalesPricingSnapshot(salesOrderId, itemId, unitPrice, discountRate, netPrice);
        return snapshotRepository.save(snapshot);
    }
}
