package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkforceAdvanceRepository extends JpaRepository<WorkforceAdvance, String> {
    List<WorkforceAdvance> findByWorkerId(String workerId);
    List<WorkforceAdvance> findByContractorId(String contractorId);
    List<WorkforceAdvance> findByEmployeeIdOrderByCreatedAtAsc(String employeeId);
    List<WorkforceAdvance> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);
    List<WorkforceAdvance> findByStatus(String status);
}
