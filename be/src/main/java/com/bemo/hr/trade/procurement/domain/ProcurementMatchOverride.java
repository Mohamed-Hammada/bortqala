package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "procurement_match_overrides")
public class ProcurementMatchOverride {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "match_id", nullable = false, length = 36)
    private String matchId;
    @Column(name = "override_reason", nullable = false, length = 255)
    private String overrideReason;
    @Column(name = "approved_by", nullable = false, length = 100)
    private String approvedBy;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.APPROVED;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected ProcurementMatchOverride() {
    }

    public ProcurementMatchOverride(String matchId, String overrideReason, String approvedBy) {
        this.id = UUID.randomUUID().toString();
        this.matchId = matchId;
        this.overrideReason = overrideReason;
        this.approvedBy = approvedBy;
        this.status = Status.APPROVED;
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

    public String getMatchId() {
        return matchId;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public String getApprovedBy() {
        return approvedBy;
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
        APPROVED
    }
}
