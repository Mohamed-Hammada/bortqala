package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientBillingPeriodRepository extends JpaRepository<ClientBillingPeriod, String> {

    Optional<ClientBillingPeriod> findByClientPartyIdAndPeriod(String clientPartyId, String period);

    List<ClientBillingPeriod> findByClientPartyIdOrderByPeriodDesc(String clientPartyId);

    List<ClientBillingPeriod> findAllByOrderByPeriodDesc();
}