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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "rfq_headers")
public class RfqHeader {

    public enum Status {
        DRAFT, ISSUED, EVALUATING, AWARDED, CANCELLED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "rfq_number", nullable = false, length = 50)
    private String rfqNumber;

    @Column(name = "requisition_id", length = 36)
    private String requisitionId;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

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

    protected RfqHeader() {}

    public RfqHeader(String rfqNumber, String requisitionId, LocalDate issueDate, LocalDate dueDate) {
        this.id = UUID.randomUUID().toString();
        this.rfqNumber = rfqNumber;
        this.requisitionId = requisitionId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.status = Status.DRAFT;
    }

    public void issue() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("Only DRAFT RFQs can be issued");
        }
        this.status = Status.ISSUED;
    }

    public void startEvaluation() {
        if (this.status != Status.ISSUED) {
            throw new IllegalStateException("Only ISSUED RFQs can enter evaluation");
        }
        this.status = Status.EVALUATING;
    }

    public void award() {
        if (this.status != Status.EVALUATING && this.status != Status.ISSUED) {
            throw new IllegalStateException("Only ISSUED or EVALUATING RFQs can be awarded");
        }
        this.status = Status.AWARDED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getRfqNumber() { return rfqNumber; }
    public String getRequisitionId() { return requisitionId; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
