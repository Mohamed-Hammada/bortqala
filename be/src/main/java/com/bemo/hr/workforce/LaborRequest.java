package com.bemo.hr.workforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "labor_requests")
@Getter
public class LaborRequest {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "request_number", nullable = false, length = 50) private String requestNumber;
    @Column(name = "request_date", nullable = false) private Instant requestDate;
    @Column(name = "branch_id", length = 36) private String branchId;
    @Column(name = "shift_name", length = 50) private String shiftName;
    @Column(name = "contractor_id", nullable = false, length = 36) private String contractorId;
    @Column(nullable = false, length = 30) private String status;
    @Column(length = 1000) private String notes;
    @Column(name = "created_by", length = 160) private String createdBy;
    @Column(name = "approved_by", length = 160) private String approvedBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected LaborRequest() { }

    public LaborRequest(String requestNumber, Instant requestDate, String branchId, String shiftName,
                        String contractorId, String status, String notes, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.requestNumber = requestNumber != null ? requestNumber.strip().toUpperCase() : "REQ-" + UUID.randomUUID().toString().substring(0, 6);
        this.requestDate = requestDate != null ? requestDate : Instant.now();
        this.branchId = branchId;
        this.shiftName = shiftName;
        this.contractorId = contractorId;
        this.status = status != null ? status.strip().toUpperCase() : "DRAFT";
        this.notes = notes;
        this.createdBy = createdBy;
    }

    public void updateStatus(String newStatus, String approver) {
        this.status = newStatus.strip().toUpperCase();
        if ("APPROVED".equalsIgnoreCase(newStatus) || "COMPLETED".equalsIgnoreCase(newStatus)) {
            this.approvedBy = approver;
        }
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

    public long getVersion() { return version; }
}
