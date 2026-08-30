package com.bemo.hr.trade.procurement.request.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
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
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "purchase_requests")
public class PurchaseRequest {

    public enum Status {DRAFT, SUBMITTED, APPROVED, REJECTED, CONVERTED, CANCELLED}

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "request_number", nullable = false, length = 50)
    private String requestNumber;
    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;
    @Column(name = "department_id", length = 36)
    private String departmentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;
    @Column(name = "needed_by")
    private LocalDate neededBy;
    @Column(length = 500)
    private String notes;
    @Column(name = "converted_po_id", length = 36)
    private String convertedPoId;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected PurchaseRequest() {
    }

    public PurchaseRequest(String requestNumber, String requestedBy, String departmentId,
                           LocalDate neededBy, String notes) {
        this.id = UUID.randomUUID().toString();
        this.requestNumber = requestNumber;
        this.requestedBy = requestedBy;
        this.departmentId = departmentId == null || departmentId.isBlank() ? null : departmentId.strip();
        this.neededBy = neededBy;
        this.notes = notes == null || notes.isBlank() ? null : notes.strip();
        this.status = Status.DRAFT;
    }

    private void requireStatus(Status expected, String action) {
        if (this.status != expected)
            throw new BusinessRuleException(action + " is only allowed while the purchase request is " + expected + ".",
                    "PR_INVALID_STATE", HttpStatus.CONFLICT);
    }

    public void submit() {
        requireStatus(Status.DRAFT, "Submission");
        this.status = Status.SUBMITTED;
    }

    public void approve() {
        requireStatus(Status.SUBMITTED, "Approval");
        this.status = Status.APPROVED;
    }

    public void reject() {
        requireStatus(Status.SUBMITTED, "Rejection");
        this.status = Status.REJECTED;
    }

    public void cancel() {
        if (this.status != Status.DRAFT && this.status != Status.SUBMITTED)
            throw new BusinessRuleException("Only DRAFT or SUBMITTED purchase requests can be cancelled.",
                    "PR_INVALID_STATE", HttpStatus.CONFLICT);
        this.status = Status.CANCELLED;
    }

    public void markConverted(String purchaseOrderId) {
        if (this.status == Status.CONVERTED || this.convertedPoId != null)
            throw new BusinessRuleException("This purchase request was already converted to a purchase order.",
                    "PR_ALREADY_CONVERTED", HttpStatus.CONFLICT);
        requireStatus(Status.APPROVED, "Conversion");
        this.convertedPoId = purchaseOrderId;
        this.status = Status.CONVERTED;
    }

    public void editDraft(String requestedBy, String departmentId, LocalDate neededBy, String notes) {
        requireStatus(Status.DRAFT, "Editing");
        if (requestedBy != null && !requestedBy.isBlank()) this.requestedBy = requestedBy.strip();
        if (departmentId != null) this.departmentId = departmentId.isBlank() ? null : departmentId.strip();
        if (neededBy != null) this.neededBy = neededBy;
        if (notes != null) this.notes = notes.isBlank() ? null : notes.strip();
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

    public String getRequestNumber() {
        return requestNumber;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDate getNeededBy() {
        return neededBy;
    }

    public String getNotes() {
        return notes;
    }

    public String getConvertedPoId() {
        return convertedPoId;
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
}
