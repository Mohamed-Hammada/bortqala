package com.bemo.hr.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, String> {
    List<ApprovalDecision> findByInstanceIdOrderByDecidedAtAsc(String instanceId);

    boolean existsByInstanceIdAndStepIdAndDecidedByIgnoreCaseAndDecision(String instanceId, String stepId, String decidedBy, String decision);

    long countByInstanceIdAndStepIdAndDecision(String instanceId, String stepId, String decision);
}
