package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.SalesDeliveryHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesDeliveryHeaderRepository extends JpaRepository<SalesDeliveryHeader, String> {
    List<SalesDeliveryHeader> findBySalesOrderId(String salesOrderId);
    List<SalesDeliveryHeader> findByCustomerId(String customerId);
    Optional<SalesDeliveryHeader> findByOperationId(String operationId);
    boolean existsByDeliveryNumberIgnoreCase(String deliveryNumber);
}
