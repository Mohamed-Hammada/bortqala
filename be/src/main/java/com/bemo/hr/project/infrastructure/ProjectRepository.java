package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.Project;
import com.bemo.hr.project.domain.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    Optional<Project> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);

    List<Project> findAllByOrderByCreatedAtDesc();

    List<Project> findTop10ByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc(String name, String code);

    List<Project> findByStatusOrderByCreatedAtDesc(ProjectStatus status);

    List<Project> findByCompanyIdOrderByCreatedAtDesc(String companyId);

    /**
     * 2026-09-07 remediation (branch-filtering hardening): Project.branchId is real and populated
     * — used to give the Owner Cockpit's project budget/actual/WIP figures real, branch-scoped
     * results when a specific branch is requested.
     */
    List<Project> findByBranchIdOrderByCreatedAtDesc(String branchId);

    long countByStatus(ProjectStatus status);

    @Query("SELECT COALESCE(SUM(p.contractValue), 0) FROM Project p")
    BigDecimal sumTotalContractValue();
}
