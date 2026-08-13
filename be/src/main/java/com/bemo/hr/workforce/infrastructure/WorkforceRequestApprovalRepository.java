package com.bemo.hr.workforce.infrastructure;

import com.bemo.hr.workforce.domain.WorkforceRequestApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkforceRequestApprovalRepository extends JpaRepository<WorkforceRequestApproval, String> {
    List<WorkforceRequestApproval> findByRequestId(String requestId);
}
