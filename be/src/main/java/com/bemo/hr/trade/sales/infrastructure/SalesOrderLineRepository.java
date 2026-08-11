package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.SalesOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderLineRepository extends JpaRepository<SalesOrderLine, String> {
    List<SalesOrderLine> findBySalesOrderId(String salesOrderId);
}
