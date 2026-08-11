package com.bemo.hr.manufacturing.production.infrastructure;

import com.bemo.hr.manufacturing.production.domain.ProductionReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionReceiptRepository extends JpaRepository<ProductionReceipt, String> {
    List<ProductionReceipt> findByProductionOrderId(String productionOrderId);
}
