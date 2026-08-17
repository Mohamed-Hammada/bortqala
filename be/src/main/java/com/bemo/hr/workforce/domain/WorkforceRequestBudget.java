package com.bemo.hr.workforce.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "workforce_request_budgets")
public class WorkforceRequestBudget {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;
    @Column(name = "department_id", nullable = false, length = 36)
    private String departmentId;
    @Column(name = "budget_id", nullable = false, length = 36)
    private String budgetId;
    @Column(name = "allocated_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal allocatedAmount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ALLOCATED;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected WorkforceRequestBudget() {
    }

    public WorkforceRequestBudget(String requestId, String departmentId, String budgetId, BigDecimal allocatedAmount) {
        this.id = UUID.randomUUID().toString();
        this.requestId = requestId;
        this.departmentId = departmentId;
        this.budgetId = budgetId;
        this.allocatedAmount = allocatedAmount;
        this.status = Status.ALLOCATED;
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

    public String getRequestId() {
        return requestId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getBudgetId() {
        return budgetId;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
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
        ALLOCATED
    }
}
