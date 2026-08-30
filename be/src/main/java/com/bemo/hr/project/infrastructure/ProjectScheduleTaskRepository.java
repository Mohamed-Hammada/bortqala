package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.ProjectScheduleTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectScheduleTaskRepository extends JpaRepository<ProjectScheduleTask, String> {

    List<ProjectScheduleTask> findByScheduleIdOrderBySortOrderAsc(String scheduleId);

    Optional<ProjectScheduleTask> findByScheduleIdAndWbsNodeId(String scheduleId, String wbsNodeId);

    void deleteByScheduleId(String scheduleId);
}
