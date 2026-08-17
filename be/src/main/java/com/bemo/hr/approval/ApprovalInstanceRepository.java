package com.bemo.hr.approval;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, String> {
    Optional<ApprovalInstance> findByDocumentTypeAndDocumentId(String documentType, String documentId);

    List<ApprovalInstance> findByStatusIn(List<String> statuses);

    List<ApprovalInstance> findBySubmittedBy(String submittedBy);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ApprovalInstance> findByStatusInAndStepDueAtBeforeAndEscalatedAtIsNull(List<String> statuses, Instant dueAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ApprovalInstance i where i.id = :id")
    Optional<ApprovalInstance> findByIdForUpdate(@Param("id") String id);
}
