package com.bemo.hr.budget.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, String> {

    List<Budget> findByActiveTrueOrderByFiscalYearDescPeriodMonthAsc();

    List<Budget> findAllByOrderByFiscalYearDescPeriodMonthAsc();

    List<Budget> findByDepartmentIdAndActiveTrue(String departmentId);

    Optional<Budget> findByIdAndActiveTrue(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Budget b where b.id = :id")
    Optional<Budget> findByIdForUpdate(String id);

    boolean existsByDepartmentIdAndFiscalYearAndPeriodTypeAndActiveTrue(
            String departmentId, int fiscalYear, BudgetPeriodType periodType);
}
