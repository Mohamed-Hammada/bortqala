package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.ProductionVarianceClose;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionVarianceCloseRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ManufacturingVarianceCloseService {

    private final ProductionVarianceCloseRepository repository;

    public ManufacturingVarianceCloseService(ProductionVarianceCloseRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProductionVarianceClose calculateAndCloseVariance(String workOrderId, BigDecimal standardCost, BigDecimal actualCost) {
        ProductionVarianceClose close = repository.findByWorkOrderId(workOrderId)
                .orElseGet(() -> new ProductionVarianceClose(workOrderId, standardCost, actualCost));
        return repository.save(close);
    }

    @Transactional(readOnly = true)
    public ProductionVarianceClose getVarianceClose(String workOrderId) {
        return repository.findByWorkOrderId(workOrderId)
                .orElseThrow(() -> new BusinessRuleException("Production variance close record not found", "VARIANCE_CLOSE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
