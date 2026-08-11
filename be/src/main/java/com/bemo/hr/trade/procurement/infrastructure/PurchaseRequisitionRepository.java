package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.PurchaseRequisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRequisitionRepository extends JpaRepository<PurchaseRequisition, String> {
    List<PurchaseRequisition> findByStatus(PurchaseRequisition.Status status);
}
