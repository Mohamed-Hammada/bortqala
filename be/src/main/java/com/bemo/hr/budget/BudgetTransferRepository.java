package com.bemo.hr.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetTransferRepository extends JpaRepository<BudgetTransfer, String> {
    List<BudgetTransfer> findBySourceBudgetIdOrTargetBudgetId(String sourceBudgetId, String targetBudgetId);
}
