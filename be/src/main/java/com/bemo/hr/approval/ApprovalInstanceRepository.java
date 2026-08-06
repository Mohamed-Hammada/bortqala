package com.bemo.hr.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, String> {
    Optional<ApprovalInstance> findByDocumentTypeAndDocumentId(String documentType, String documentId);
    List<ApprovalInstance> findByStatusIn(List<String> statuses);
    List<ApprovalInstance> findBySubmittedBy(String submittedBy);
}
