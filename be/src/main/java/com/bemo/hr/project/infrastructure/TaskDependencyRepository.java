package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, String> {

    List<TaskDependency> findByScheduleId(String scheduleId);

    List<TaskDependency> findByPredecessorTaskId(String predecessorTaskId);

    List<TaskDependency> findBySuccessorTaskId(String successorTaskId);

    Optional<TaskDependency> findByScheduleIdAndPredecessorTaskIdAndSuccessorTaskId(
            String scheduleId, String predecessorTaskId, String successorTaskId);

    void deleteByScheduleId(String scheduleId);

    void deleteByPredecessorTaskIdOrSuccessorTaskId(String predecessorTaskId, String successorTaskId);
}
