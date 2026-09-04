package com.bemo.hr.trade.fieldsales.infrastructure;

import com.bemo.hr.trade.fieldsales.domain.FieldSalesOfflineTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FieldSalesOfflineTransactionRepository extends JpaRepository<FieldSalesOfflineTransaction, String> {

    Optional<FieldSalesOfflineTransaction> findByClientOfflineId(String clientOfflineId);

    boolean existsByClientOfflineId(String clientOfflineId);

    List<FieldSalesOfflineTransaction> findAllBySalesRepUserIdOrderByCreatedAtDesc(String salesRepUserId);
}
