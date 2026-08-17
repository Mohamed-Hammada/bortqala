package com.bemo.hr.manufacturing.production.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "material_issue_headers")
public class MaterialIssueHeader {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "issue_number", nullable = false, length = 50)
    private String issueNumber;
    @Column(name = "production_order_id", nullable = false, length = 36)
    private String productionOrderId;
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ISSUED;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected MaterialIssueHeader() {
    }

    public MaterialIssueHeader(String issueNumber, String productionOrderId, LocalDate issueDate) {
        this.id = UUID.randomUUID().toString();
        this.issueNumber = issueNumber;
        this.productionOrderId = productionOrderId;
        this.issueDate = issueDate;
        this.status = Status.ISSUED;
    }

    public void cancel() {
        if (this.status != Status.ISSUED) {
            throw new IllegalStateException("Only ISSUED material issues can be cancelled");
        }
        this.status = Status.CANCELLED;
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

    public String getIssueNumber() {
        return issueNumber;
    }

    public String getProductionOrderId() {
        return productionOrderId;
    }

    public LocalDate getIssueDate() {
        return issueDate;
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
        ISSUED, CANCELLED
    }
}
