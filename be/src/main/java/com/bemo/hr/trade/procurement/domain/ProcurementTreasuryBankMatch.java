package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "procurement_treasury_bank_matches")
public class ProcurementTreasuryBankMatch {

    public enum Status {
        MATCHED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "payment_id", nullable = false, length = 36)
    private String paymentId;

    @Column(name = "bank_transaction_id", nullable = false, length = 36)
    private String bankTransactionId;

    @Column(name = "matched_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal matchedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.MATCHED;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ProcurementTreasuryBankMatch() {}

    public ProcurementTreasuryBankMatch(String paymentId, String bankTransactionId, BigDecimal matchedAmount) {
        this.id = UUID.randomUUID().toString();
        this.paymentId = paymentId;
        this.bankTransactionId = bankTransactionId;
        this.matchedAmount = matchedAmount;
        this.status = Status.MATCHED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getPaymentId() { return paymentId; }
    public String getBankTransactionId() { return bankTransactionId; }
    public BigDecimal getMatchedAmount() { return matchedAmount; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
