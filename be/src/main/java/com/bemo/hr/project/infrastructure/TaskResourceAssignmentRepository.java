package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.TaskResourceAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskResourceAssignmentRepository extends JpaRepository<TaskResourceAssignment, String> {

    List<TaskResourceAssignment> findByTaskId(String taskId);

    void deleteByTaskId(String taskId);
}
