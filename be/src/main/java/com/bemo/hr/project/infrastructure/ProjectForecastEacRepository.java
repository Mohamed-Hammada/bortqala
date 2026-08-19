package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.ProjectForecastEac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectForecastEacRepository extends JpaRepository<ProjectForecastEac, String> {

    List<ProjectForecastEac> findByProjectId(String projectId);

    Optional<ProjectForecastEac> findByProjectIdAndWbsNodeId(String projectId, String wbsNodeId);
}
