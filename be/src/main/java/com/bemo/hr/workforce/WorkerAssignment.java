package com.bemo.hr.workforce;

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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "worker_assignments")
public class WorkerAssignment {

    public enum Status {
        PROPOSED, ACCEPTED, REJECTED, REPLACED, COMPLETED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "dispatch_id", nullable = false, length = 36)
    private String dispatchId;

    @Column(name = "worker_id", nullable = false, length = 36)
    private String workerId;

    @Column(name = "request_line_id", length = 36)
    private String requestLineId;

    @Column(name = "contractor_id", nullable = false, length = 36)
    private String contractorId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "agreed_rate_snapshot", nullable = false, precision = 15, scale = 2)
    private BigDecimal agreedRateSnapshot;

    @Column(name = "agreed_hours_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal agreedHoursSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PROPOSED;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected WorkerAssignment() {}

    public WorkerAssignment(String dispatchId, String workerId, String requestLineId, String contractorId,
                            LocalDate fromDate, LocalDate toDate, BigDecimal agreedRateSnapshot, BigDecimal agreedHoursSnapshot) {
        this.id = UUID.randomUUID().toString();
        this.dispatchId = dispatchId;
        this.workerId = workerId;
        this.requestLineId = requestLineId;
        this.contractorId = contractorId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.agreedRateSnapshot = agreedRateSnapshot;
        this.agreedHoursSnapshot = agreedHoursSnapshot;
        this.status = Status.PROPOSED;
    }

    public void accept() {
        if (this.status != Status.PROPOSED) {
            throw new IllegalStateException("Only PROPOSED assignments can be accepted");
        }
        this.status = Status.ACCEPTED;
    }

    public void reject(String reason) {
        if (this.status != Status.PROPOSED) {
            throw new IllegalStateException("Only PROPOSED assignments can be rejected");
        }
        this.status = Status.REJECTED;
        this.rejectionReason = reason;
    }

    public void replace() {
        if (this.status != Status.REJECTED && this.status != Status.PROPOSED) {
            throw new IllegalStateException("Cannot replace an assignment that is " + this.status);
        }
        this.status = Status.REPLACED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getDispatchId() { return dispatchId; }
    public String getWorkerId() { return workerId; }
    public String getRequestLineId() { return requestLineId; }
    public String getContractorId() { return contractorId; }
    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate() { return toDate; }
    public BigDecimal getAgreedRateSnapshot() { return agreedRateSnapshot; }
    public BigDecimal getAgreedHoursSnapshot() { return agreedHoursSnapshot; }
    public Status getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
