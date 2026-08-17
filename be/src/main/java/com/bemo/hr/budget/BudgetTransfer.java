package com.bemo.hr.budget;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budget_transfers")
public class BudgetTransfer {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "transfer_number", nullable = false, length = 50)
    private String transferNumber;
    @Column(name = "source_budget_id", nullable = false, length = 36)
    private String sourceBudgetId;
    @Column(name = "target_budget_id", nullable = false, length = 36)
    private String targetBudgetId;
    @Column(name = "transfer_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal transferAmount;
    @Column(length = 500)
    private String reason;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected BudgetTransfer() {
    }

    public BudgetTransfer(String transferNumber, String sourceBudgetId, String targetBudgetId, BigDecimal transferAmount, String reason) {
        this.id = UUID.randomUUID().toString();
        this.transferNumber = transferNumber;
        this.sourceBudgetId = sourceBudgetId;
        this.targetBudgetId = targetBudgetId;
        this.transferAmount = transferAmount;
        this.reason = reason;
        this.status = Status.DRAFT;
    }

    public void approve() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("Only DRAFT budget transfers can be approved");
        }
        this.status = Status.APPROVED;
    }

    public void reject() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("Only DRAFT budget transfers can be rejected");
        }
        this.status = Status.REJECTED;
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

    public String getTransferNumber() {
        return transferNumber;
    }

    public String getSourceBudgetId() {
        return sourceBudgetId;
    }

    public String getTargetBudgetId() {
        return targetBudgetId;
    }

    public BigDecimal getTransferAmount() {
        return transferAmount;
    }

    public String getReason() {
        return reason;
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
        DRAFT, APPROVED, REJECTED
    }
}
