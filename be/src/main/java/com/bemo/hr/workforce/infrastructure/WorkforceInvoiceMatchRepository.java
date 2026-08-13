package com.bemo.hr.workforce.infrastructure;

import com.bemo.hr.workforce.domain.WorkforceInvoiceMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkforceInvoiceMatchRepository extends JpaRepository<WorkforceInvoiceMatch, String> {
    Optional<WorkforceInvoiceMatch> findBySettlementId(String settlementId);
}
