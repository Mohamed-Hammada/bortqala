package com.bemo.hr.expenses.infrastructure;

import com.bemo.hr.expenses.domain.ExpenseClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, String> {
    List<ExpenseClaim> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);
    List<ExpenseClaim> findByStatusOrderByCreatedAtDesc(String status);
    boolean existsByIdAndEmployeeId(String id, String employeeId);
}
