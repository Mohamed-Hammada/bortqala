package com.bemo.hr.workforce.infrastructure;

import com.bemo.hr.workforce.domain.WorkforceTreasuryMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkforceTreasuryMatchRepository extends JpaRepository<WorkforceTreasuryMatch, String> {
    Optional<WorkforceTreasuryMatch> findByPaymentId(String paymentId);
}
