package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "customer_receipt_bank_matches")
public class CustomerReceiptBankMatch {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "receipt_id", nullable = false, length = 36)
    private String receiptId;
    @Column(name = "bank_transaction_id", nullable = false, length = 36)
    private String bankTransactionId;
    @Column(name = "matched_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal matchedAmount;
    @Column(name = "matched_at", nullable = false)
    private long matchedAt;
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

    protected CustomerReceiptBankMatch() {
    }

    public CustomerReceiptBankMatch(String receiptId, String bankTransactionId, BigDecimal matchedAmount) {
        this.id = UUID.randomUUID().toString();
        this.receiptId = receiptId;
        this.bankTransactionId = bankTransactionId;
        this.matchedAmount = matchedAmount;
        this.matchedAt = System.currentTimeMillis();
        this.status = Status.MATCHED;
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public String getBankTransactionId() {
        return bankTransactionId;
    }

    public BigDecimal getMatchedAmount() {
        return matchedAmount;
    }

    public long getMatchedAt() {
        return matchedAt;
    }

    public Status getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public enum Status {
        MATCHED
    }
}
