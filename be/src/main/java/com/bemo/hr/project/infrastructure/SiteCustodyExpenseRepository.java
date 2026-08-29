package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.SiteCustodyExpense;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteCustodyExpenseRepository extends JpaRepository<SiteCustodyExpense, String> {
    List<SiteCustodyExpense> findByCustodyIdOrderByExpenseDateDesc(String custodyId);
}
