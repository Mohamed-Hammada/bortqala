package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientBillingDraftLineRepository extends JpaRepository<ClientBillingDraftLine, String> {

    List<ClientBillingDraftLine> findByBillingPeriodId(String billingPeriodId);

    void deleteByBillingPeriodId(String billingPeriodId);
}