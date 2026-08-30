package com.bemo.hr.finance.paylink.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gateway_transactions")
public class GatewayTransaction {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "link_id", nullable = false, length = 36)
    private String linkId;
    @Column(name = "provider_txn_id", nullable = false, length = 200)
    private String providerTxnId;
    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;
    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected GatewayTransaction() {}

    public GatewayTransaction(String appId, String linkId, String providerTxnId,
                              String rawPayload, BigDecimal amount, Instant paidAt) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.linkId = linkId;
        this.providerTxnId = providerTxnId;
        this.rawPayload = rawPayload;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getLinkId() { return linkId; }
    public String getProviderTxnId() { return providerTxnId; }
    public String getRawPayload() { return rawPayload; }
    public BigDecimal getAmount() { return amount; }
    public Instant getPaidAt() { return paidAt; }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
    }
}
