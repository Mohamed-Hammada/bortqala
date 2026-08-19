package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.ProjectBudgetLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectBudgetLineRepository extends JpaRepository<ProjectBudgetLine, String> {

    List<ProjectBudgetLine> findByBudgetVersionIdOrderBySortOrderAsc(String budgetVersionId);

    List<ProjectBudgetLine> findByProjectId(String projectId);

    void deleteByBudgetVersionId(String budgetVersionId);
}
