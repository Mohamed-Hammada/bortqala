package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {
    List<PurchaseOrder> findAllByOrderByPoDateDescCreatedAtDesc();

    List<PurchaseOrder> findTop10ByPoNumberContainingIgnoreCaseOrderByPoDateDesc(String poNumber);

    List<PurchaseOrder> findBySupplierId(String supplierId);

    boolean existsByPoNumberIgnoreCase(String poNumber);

    boolean existsByPoNumberIgnoreCaseAndIdNot(String poNumber, String id);

    @Query("select max(p.poNumber) from PurchaseOrder p")
    Optional<String> findMaxPoNumber();
}
