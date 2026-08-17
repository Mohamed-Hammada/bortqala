package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.CollectionTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionTaskRepository extends JpaRepository<CollectionTask, String> {
    Optional<CollectionTask> findByInvoiceId(String invoiceId);

    List<CollectionTask> findAllByOrderByNextActionDateAscCreatedAtAsc();
}
