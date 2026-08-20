package com.bemo.hr.crm.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crm_activities")
public class CrmActivity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "lead_id", length = 36, nullable = false)
    private String leadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", length = 30, nullable = false)
    private CrmActivityType activityType;

    @Column(name = "summary", length = 200, nullable = false)
    private String summary;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "due_date")
    private Long dueDate;

    @Column(name = "completed_at")
    private Long completedAt;

    @Column(name = "assigned_to_user_id", length = 36)
    private String assignedToUserId;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected CrmActivity() {}

    public CrmActivity(String leadId, CrmActivityType activityType, String summary,
                       String details, Long dueDate, String assignedToUserId) {
        this.id = UUID.randomUUID().toString();
        this.leadId = leadId;
        this.activityType = activityType != null ? activityType : CrmActivityType.NOTE;
        this.summary = summary;
        this.details = details;
        this.dueDate = dueDate;
        this.assignedToUserId = assignedToUserId;
        long now = Instant.now().toEpochMilli();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void complete() {
        this.completedAt = Instant.now().toEpochMilli();
        this.updatedAt = this.completedAt;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getLeadId() { return leadId; }
    public CrmActivityType getActivityType() { return activityType; }
    public String getSummary() { return summary; }
    public String getDetails() { return details; }
    public Long getDueDate() { return dueDate; }
    public Long getCompletedAt() { return completedAt; }
    public String getAssignedToUserId() { return assignedToUserId; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
