package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.ProcurementMatchOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcurementMatchOverrideRepository extends JpaRepository<ProcurementMatchOverride, String> {
    Optional<ProcurementMatchOverride> findByMatchId(String matchId);
}
