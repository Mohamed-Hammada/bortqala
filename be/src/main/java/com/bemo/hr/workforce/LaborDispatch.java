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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "labor_dispatches")
public class LaborDispatch {

    public enum Status {
        DRAFT, DISPATCHED, ACCEPTED, CANCELLED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    @Column(name = "contractor_id", nullable = false, length = 36)
    private String contractorId;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

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

    protected LaborDispatch() {}

    public LaborDispatch(String requestId, String contractorId, LocalDate dispatchDate) {
        this.id = UUID.randomUUID().toString();
        this.requestId = requestId;
        this.contractorId = contractorId;
        this.dispatchDate = dispatchDate;
        this.status = Status.DRAFT;
    }

    public void dispatch() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("Only DRAFT dispatches can be dispatched");
        }
        this.status = Status.DISPATCHED;
    }

    public void accept() {
        if (this.status != Status.DISPATCHED) {
            throw new IllegalStateException("Only DISPATCHED dispatches can be accepted");
        }
        this.status = Status.ACCEPTED;
    }

    public void cancel() {
        if (this.status == Status.ACCEPTED) {
            throw new IllegalStateException("Cannot cancel an ACCEPTED dispatch");
        }
        this.status = Status.CANCELLED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getRequestId() { return requestId; }
    public String getContractorId() { return contractorId; }
    public LocalDate getDispatchDate() { return dispatchDate; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
