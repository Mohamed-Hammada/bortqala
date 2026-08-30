package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.BudgetVersionStatus;
import com.bemo.hr.project.domain.ProjectBudgetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectBudgetVersionRepository extends JpaRepository<ProjectBudgetVersion, String> {

    List<ProjectBudgetVersion> findByProjectIdOrderByVersionNumberDesc(String projectId);

    Optional<ProjectBudgetVersion> findByProjectIdAndStatus(String projectId, BudgetVersionStatus status);

    Optional<ProjectBudgetVersion> findByProjectIdAndVersionNumber(String projectId, int versionNumber);
}
