package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.ProjectSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectScheduleRepository extends JpaRepository<ProjectSchedule, String> {

    Optional<ProjectSchedule> findByProjectId(String projectId);
}
