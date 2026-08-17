package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "procurement_budget_approvals")
public class ProcurementBudgetApproval {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "requisition_id", nullable = false, length = 36)
    private String requisitionId;
    @Column(name = "budget_id", nullable = false, length = 36)
    private String budgetId;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.APPROVED;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected ProcurementBudgetApproval() {
    }

    public ProcurementBudgetApproval(String requisitionId, String budgetId, BigDecimal amount) {
        this.id = UUID.randomUUID().toString();
        this.requisitionId = requisitionId;
        this.budgetId = budgetId;
        this.amount = amount;
        this.status = Status.APPROVED;
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

    public String getRequisitionId() {
        return requisitionId;
    }

    public String getBudgetId() {
        return budgetId;
    }

    public BigDecimal getAmount() {
        return amount;
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
        APPROVED
    }
}
