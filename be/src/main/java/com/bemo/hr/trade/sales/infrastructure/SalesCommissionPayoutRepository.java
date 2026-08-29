package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.SalesCommissionPayout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesCommissionPayoutRepository extends JpaRepository<SalesCommissionPayout, String> {
    Optional<SalesCommissionPayout> findByRepIdAndPeriod(String repId, String period);
}