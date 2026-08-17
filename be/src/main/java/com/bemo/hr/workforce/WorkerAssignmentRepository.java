package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkerAssignmentRepository extends JpaRepository<WorkerAssignment, String> {
    List<WorkerAssignment> findByDispatchId(String dispatchId);

    List<WorkerAssignment> findByWorkerId(String workerId);
}
