package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.CustomerReturnHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerReturnHeaderRepository extends JpaRepository<CustomerReturnHeader, String> {
    List<CustomerReturnHeader> findBySalesOrderId(String salesOrderId);
    List<CustomerReturnHeader> findByCustomerId(String customerId);
    Optional<CustomerReturnHeader> findByOperationId(String operationId);
    boolean existsByReturnNumberIgnoreCase(String returnNumber);
}
