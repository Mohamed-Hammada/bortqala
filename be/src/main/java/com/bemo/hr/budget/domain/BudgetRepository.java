package com.bemo.hr.budget.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, String> {

    List<Budget> findByActiveTrueOrderByFiscalYearDescPeriodMonthAsc();

    List<Budget> findAllByOrderByFiscalYearDescPeriodMonthAsc();

    List<Budget> findByDepartmentIdAndActiveTrue(String departmentId);

    Optional<Budget> findByIdAndActiveTrue(String id);

    boolean existsByDepartmentIdAndFiscalYearAndPeriodTypeAndActiveTrue(
            String departmentId, int fiscalYear, BudgetPeriodType periodType);
}
