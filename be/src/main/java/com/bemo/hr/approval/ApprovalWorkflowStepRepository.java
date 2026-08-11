package com.bemo.hr.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalWorkflowStepRepository extends JpaRepository<ApprovalWorkflowStep, String> {
    List<ApprovalWorkflowStep> findByWorkflowDefinitionIdOrderByStepOrderAsc(String workflowDefinitionId);
    Optional<ApprovalWorkflowStep> findByWorkflowDefinitionIdAndStepOrder(String workflowDefinitionId, int stepOrder);
    void deleteByWorkflowDefinitionId(String workflowDefinitionId);
}
