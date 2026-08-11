package com.bemo.hr.budget.infrastructure;

import com.bemo.hr.budget.domain.BudgetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetVersionRepository extends JpaRepository<BudgetVersion, String> {
    List<BudgetVersion> findByFiscalYear(int fiscalYear);
}
