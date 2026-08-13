package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.ProcurementBudgetApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcurementBudgetApprovalRepository extends JpaRepository<ProcurementBudgetApproval, String> {
    Optional<ProcurementBudgetApproval> findByRequisitionId(String requisitionId);
}
