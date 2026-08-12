package com.bemo.hr.workforce.infrastructure;

import com.bemo.hr.workforce.domain.WorkforceGlPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkforceGlPostingRepository extends JpaRepository<WorkforceGlPosting, String> {
    Optional<WorkforceGlPosting> findBySettlementId(String settlementId);
}
