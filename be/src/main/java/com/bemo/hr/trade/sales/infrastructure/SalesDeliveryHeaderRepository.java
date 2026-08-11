package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.SalesDeliveryHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesDeliveryHeaderRepository extends JpaRepository<SalesDeliveryHeader, String> {
    List<SalesDeliveryHeader> findBySalesOrderId(String salesOrderId);
    List<SalesDeliveryHeader> findByCustomerId(String customerId);
}
