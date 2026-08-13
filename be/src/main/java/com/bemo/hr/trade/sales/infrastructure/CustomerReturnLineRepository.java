package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.CustomerReturnLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface CustomerReturnLineRepository extends JpaRepository<CustomerReturnLine, String> {
    List<CustomerReturnLine> findByReturnIdOrderByCreatedAtAsc(String returnId);

    @Query("select coalesce(sum(l.quantity), 0) from CustomerReturnLine l where l.deliveryLineId = :deliveryLineId")
    BigDecimal returnedQuantity(@Param("deliveryLineId") String deliveryLineId);
}
