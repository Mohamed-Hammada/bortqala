package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.CustomerCreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CustomerCreditNoteRepository extends JpaRepository<CustomerCreditNote, String> {
    Optional<CustomerCreditNote> findByOperationId(String operationId);
    List<CustomerCreditNote> findBySalesOrderIdOrderByCreditDateDesc(String salesOrderId);
}
