package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.SalesDeliveryLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesDeliveryLineRepository extends JpaRepository<SalesDeliveryLine, String> {
    List<SalesDeliveryLine> findByDeliveryIdOrderByCreatedAtAsc(String deliveryId);

    /**
     * 2026-09-07 remediation (Performance Review P-1): Executive Analytics used to call
     * {@code findAll()} and convert every row's epoch-millis {@code createdAt} to a
     * {@code LocalDate} in Java just to filter it into the requested period. Pushes the
     * epoch-millis range filter into SQL. Supported by {@code idx_sales_delivery_lines_app_created_at}.
     */
    List<SalesDeliveryLine> findByCreatedAtBetween(long startInclusive, long endInclusive);
}
