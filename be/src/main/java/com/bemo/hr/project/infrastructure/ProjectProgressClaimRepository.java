package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.ClaimStatus;
import com.bemo.hr.project.domain.ClaimType;
import com.bemo.hr.project.domain.ProjectProgressClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectProgressClaimRepository extends JpaRepository<ProjectProgressClaim, String> {

    List<ProjectProgressClaim> findByProjectIdOrderByClaimSequenceNumberDesc(String projectId);

    List<ProjectProgressClaim> findByProjectIdAndClaimTypeOrderByClaimSequenceNumberDesc(String projectId, ClaimType claimType);

    List<ProjectProgressClaim> findByProjectIdAndClaimTypeAndStatusOrderByClaimSequenceNumberDesc(String projectId, ClaimType claimType, ClaimStatus status);

    Optional<ProjectProgressClaim> findByClaimNumber(String claimNumber);

    @Query("SELECT COUNT(c) FROM ProjectProgressClaim c WHERE c.claimNumber LIKE CONCAT(:prefix, '-%')")
    long countByNumberPrefix(@Param("prefix") String prefix);

    @Query("SELECT c FROM ProjectProgressClaim c WHERE c.projectId = :projectId AND c.claimType = :claimType AND c.claimSequenceNumber < :seqNum AND c.status IN ('CERTIFIED', 'POSTED_FINANCE', 'PAID') ORDER BY c.claimSequenceNumber DESC LIMIT 1")
    Optional<ProjectProgressClaim> findPreviousApprovedClaim(
            @Param("projectId") String projectId,
            @Param("claimType") ClaimType claimType,
            @Param("seqNum") int seqNum
    );
}
