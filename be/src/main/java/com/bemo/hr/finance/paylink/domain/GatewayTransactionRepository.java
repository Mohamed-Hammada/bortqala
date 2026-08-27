package com.bemo.hr.finance.paylink.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GatewayTransactionRepository extends JpaRepository<GatewayTransaction, String> {
    Optional<GatewayTransaction> findByAppIdAndProviderTxnId(String appId, String providerTxnId);
}
