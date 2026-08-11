package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.SourcingAward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SourcingAwardRepository extends JpaRepository<SourcingAward, String> {
    Optional<SourcingAward> findByRfqId(String rfqId);
}
