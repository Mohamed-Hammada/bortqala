package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.PurchaseRequisitionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRequisitionLineRepository extends JpaRepository<PurchaseRequisitionLine, String> {
    List<PurchaseRequisitionLine> findByRequisitionId(String requisitionId);

    void deleteByRequisitionId(String requisitionId);
}
