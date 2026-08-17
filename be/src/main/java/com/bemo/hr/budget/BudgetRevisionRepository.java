package com.bemo.hr.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetRevisionRepository extends JpaRepository<BudgetRevision, String> {
    List<BudgetRevision> findByBudgetIdOrderByRevisionNumberDesc(String budgetId);

    boolean existsByBudgetIdAndStatus(String budgetId, BudgetRevision.Status status);
}
