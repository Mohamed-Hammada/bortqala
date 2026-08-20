package com.bemo.hr.workforce;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "labor_dispatches")
@Getter
public class LaborDispatch {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;
    @Column(name = "contractor_id", nullable = false, length = 36)
    private String contractorId;
    @Column(name = "project_id", length = 36)
    private String projectId;
    @Column(name = "wbs_node_id", length = 36)
    private String wbsNodeId;
    @Column(name = "cost_code_id", length = 36)
    private String costCodeId;
    @Column(name = "site_location", length = 160)
    private String siteLocation;
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

    public enum Status {
        DRAFT,
        DISPATCHED,
        ACCEPTED,
        CANCELLED
    }

    protected LaborDispatch() {
    }

    public LaborDispatch(String requestId, String contractorId, LocalDate dispatchDate) {
        this(requestId, contractorId, null, null, null, null, dispatchDate);
    }

    public LaborDispatch(String requestId, String contractorId, String projectId, String wbsNodeId,
                         String costCodeId, String siteLocation, LocalDate dispatchDate) {
        this.id = UUID.randomUUID().toString();
        this.requestId = requestId;
        this.contractorId = contractorId;
        this.projectId = projectId;
        this.wbsNodeId = wbsNodeId;
        this.costCodeId = costCodeId;
        this.siteLocation = siteLocation;
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

    public void assignProject(String projectId, String wbsNodeId, String costCodeId, String siteLocation) {
        this.projectId = projectId;
        this.wbsNodeId = wbsNodeId;
        this.costCodeId = costCodeId;
        this.siteLocation = siteLocation;
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
}
