package com.bemo.hr.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalWorkflowDefinitionRepository extends JpaRepository<ApprovalWorkflowDefinition, String> {
    List<ApprovalWorkflowDefinition> findByDocumentTypeAndActiveTrue(String documentType);

    Optional<ApprovalWorkflowDefinition> findByDocumentType(String documentType);
}
