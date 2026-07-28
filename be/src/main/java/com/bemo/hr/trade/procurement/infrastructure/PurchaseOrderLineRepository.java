package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.PurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, String> {
    List<PurchaseOrderLine> findByPurchaseOrderId(String purchaseOrderId);
}
