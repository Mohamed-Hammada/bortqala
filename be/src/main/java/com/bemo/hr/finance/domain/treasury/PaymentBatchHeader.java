package com.bemo.hr.finance.domain.treasury;

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
@Table(name = "payment_batch_headers")
public class PaymentBatchHeader {

    public enum SourceCategory {
        ACCOUNTS_PAYABLE, PAYROLL, WORKFORCE_CONTRACTOR
    }

    public enum Status {
        DRAFT, SUBMITTED, APPROVED, REJECTED, DISBURSED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "batch_number", nullable = false, length = 50)
    private String batchNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_category", nullable = false, length = 30)
    private SourceCategory sourceCategory;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_by", nullable = false, length = 100) private String createdBy;
    @Column(name = "approved_by", length = 100) private String approvedBy;
    @Column(name = "disbursed_by", length = 100) private String disbursedBy;
    @Column(name = "operation_id", length = 80) private String operationId;

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

    protected PaymentBatchHeader() {}

    public PaymentBatchHeader(String batchNumber, SourceCategory sourceCategory, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.batchNumber = batchNumber;
        this.sourceCategory = sourceCategory;
        this.totalAmount = BigDecimal.ZERO;
        this.createdBy = createdBy;
        this.status = Status.DRAFT;
    }

    public void submit() {
        if (this.status != Status.DRAFT) {
            throw conflict();
        }
        this.status = Status.SUBMITTED;
    }

    public void approve(String actor) {
        if (this.status != Status.SUBMITTED) {
            throw conflict();
        }
        this.status = Status.APPROVED;
        this.approvedBy = actor;
    }

    public void reject() {
        if (this.status != Status.SUBMITTED) {
            throw conflict();
        }
        this.status = Status.REJECTED;
    }

    public void disburse(String operationId, String actor) {
        if (this.status != Status.APPROVED) {
            throw conflict();
        }
        this.status = Status.DISBURSED;
        this.operationId = operationId;
        this.disbursedBy = actor;
    }

    public void deriveTotal(BigDecimal total) { this.totalAmount = total == null ? BigDecimal.ZERO : total; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getBatchNumber() { return batchNumber; }
    public SourceCategory getSourceCategory() { return sourceCategory; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCreatedBy() { return createdBy; }
    public String getApprovedBy() { return approvedBy; }
    public String getDisbursedBy() { return disbursedBy; }
    public String getOperationId() { return operationId; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    private com.bemo.hr.shared.domain.BusinessRuleException conflict(){return new com.bemo.hr.shared.domain.BusinessRuleException(
            "Payment batch action is not allowed in its current state","BATCH_STATE_INVALID",org.springframework.http.HttpStatus.CONFLICT);}
}
