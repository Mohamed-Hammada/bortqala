package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.SalesPricingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesPricingSnapshotRepository extends JpaRepository<SalesPricingSnapshot, String> {
    List<SalesPricingSnapshot> findBySalesOrderId(String salesOrderId);
    boolean existsBySalesOrderIdAndItemId(String salesOrderId, String itemId);
}
