package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.Project;
import com.bemo.hr.project.domain.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    Optional<Project> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);

    List<Project> findAllByOrderByCreatedAtDesc();

    List<Project> findByStatusOrderByCreatedAtDesc(ProjectStatus status);

    List<Project> findByCompanyIdOrderByCreatedAtDesc(String companyId);

    long countByStatus(ProjectStatus status);
}
