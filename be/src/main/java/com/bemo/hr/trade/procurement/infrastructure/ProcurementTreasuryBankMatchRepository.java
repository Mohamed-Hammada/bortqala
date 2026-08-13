package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.ProcurementTreasuryBankMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcurementTreasuryBankMatchRepository extends JpaRepository<ProcurementTreasuryBankMatch, String> {
    Optional<ProcurementTreasuryBankMatch> findByPaymentId(String paymentId);
}
