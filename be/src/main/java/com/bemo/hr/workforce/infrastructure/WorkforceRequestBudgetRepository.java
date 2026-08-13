package com.bemo.hr.workforce.infrastructure;

import com.bemo.hr.workforce.domain.WorkforceRequestBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkforceRequestBudgetRepository extends JpaRepository<WorkforceRequestBudget, String> {
    Optional<WorkforceRequestBudget> findByRequestId(String requestId);
}
