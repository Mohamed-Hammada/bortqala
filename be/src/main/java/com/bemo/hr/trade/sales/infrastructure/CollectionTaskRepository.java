package com.bemo.hr.trade.sales.infrastructure;
import com.bemo.hr.trade.sales.domain.CollectionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CollectionTaskRepository extends JpaRepository<CollectionTask,String>{Optional<CollectionTask> findByInvoiceId(String invoiceId);List<CollectionTask> findAllByOrderByNextActionDateAscCreatedAtAsc();}
