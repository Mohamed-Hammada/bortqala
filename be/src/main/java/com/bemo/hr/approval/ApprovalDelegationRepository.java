package com.bemo.hr.approval;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface ApprovalDelegationRepository extends JpaRepository<ApprovalDelegation, String> {
    List<ApprovalDelegation> findAllByOrderByStartsAtDesc();
    List<ApprovalDelegation> findByDelegateUserIdIgnoreCaseAndActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(
            String delegateUserId, Instant startsAt, Instant endsAt);
}
