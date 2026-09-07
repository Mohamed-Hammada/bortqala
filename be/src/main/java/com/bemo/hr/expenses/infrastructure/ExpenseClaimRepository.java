package com.bemo.hr.expenses.infrastructure;

import com.bemo.hr.expenses.domain.ExpenseClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, String> {
    List<ExpenseClaim> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);
    List<ExpenseClaim> findByStatusOrderByCreatedAtDesc(String status);
    boolean existsByIdAndEmployeeId(String id, String employeeId);

    /**
     * 2026-09-07 remediation (Performance Review P-1): Executive Analytics used to call
     * {@code findAll()} and filter {@code spentOn} into the period range in a Java stream. Pushes
     * the filter into SQL. Supported by {@code idx_expense_claims_app_spent_on}.
     */
    List<ExpenseClaim> findBySpentOnBetween(LocalDate start, LocalDate end);

    /**
     * 2026-09-07 remediation (branch-filtering hardening): ExpenseClaim has no branchId of its
     * own, but a real join through employeeId -> Employee.branchId exists — used to give the Owner
     * Cockpit's expense breakdown real, branch-scoped figures when a specific branch is requested.
     */
    List<ExpenseClaim> findByEmployeeIdInAndSpentOnBetween(Collection<String> employeeIds, LocalDate start, LocalDate end);
}
