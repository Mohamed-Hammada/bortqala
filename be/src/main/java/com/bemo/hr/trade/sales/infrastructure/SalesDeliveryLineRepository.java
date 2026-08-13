package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.SalesDeliveryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalesDeliveryLineRepository extends JpaRepository<SalesDeliveryLine, String> {
    List<SalesDeliveryLine> findByDeliveryIdOrderByCreatedAtAsc(String deliveryId);
}
