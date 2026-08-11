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

import java.util.UUID;

@Entity
@Table(name = "purchase_requisitions")
public class PurchaseRequisition {

    public enum Status {
        DRAFT, SUBMITTED, APPROVED, REJECTED, CONVERTED_TO_PO
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "requisition_number", nullable = false, length = 50)
    private String requisitionNumber;

    @Column(name = "department_id", length = 36)
    private String departmentId;

    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;

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

    protected PurchaseRequisition() {}

    public PurchaseRequisition(String requisitionNumber, String departmentId, String requestedBy) {
        this.id = UUID.randomUUID().toString();
        this.requisitionNumber = requisitionNumber;
        this.departmentId = departmentId;
        this.requestedBy = requestedBy;
        this.status = Status.DRAFT;
    }

    public void submit() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("Only DRAFT requisitions can be submitted");
        }
        this.status = Status.SUBMITTED;
    }

    public void approve() {
        if (this.status != Status.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED requisitions can be approved");
        }
        this.status = Status.APPROVED;
    }

    public void markConvertedToPo() {
        if (this.status != Status.APPROVED) {
            throw new IllegalStateException("Only APPROVED requisitions can be converted to PO");
        }
        this.status = Status.CONVERTED_TO_PO;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getRequisitionNumber() { return requisitionNumber; }
    public String getDepartmentId() { return departmentId; }
    public String getRequestedBy() { return requestedBy; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
