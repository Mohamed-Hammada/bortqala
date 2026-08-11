package com.bemo.hr.manufacturing.quality.domain;

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
@Table(name = "quality_dispositions")
public class QualityDisposition {

    public enum Result {
        PASSED, REJECTED, QUARANTINED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "disposition_number", nullable = false, length = 50)
    private String dispositionNumber;

    @Column(name = "plan_id", nullable = false, length = 36)
    private String planId;

    @Column(name = "inspection_id", nullable = false, length = 36)
    private String inspectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposition_result", nullable = false, length = 20)
    private Result dispositionResult;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected QualityDisposition() {}

    public QualityDisposition(String dispositionNumber, String planId, String inspectionId, Result dispositionResult, String notes) {
        this.id = UUID.randomUUID().toString();
        this.dispositionNumber = dispositionNumber;
        this.planId = planId;
        this.inspectionId = inspectionId;
        this.dispositionResult = dispositionResult;
        this.notes = notes == null ? null : notes.strip();
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getDispositionNumber() { return dispositionNumber; }
    public String getPlanId() { return planId; }
    public String getInspectionId() { return inspectionId; }
    public Result getDispositionResult() { return dispositionResult; }
    public String getNotes() { return notes; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
