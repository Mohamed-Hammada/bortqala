package com.bemo.hr.finance.domain.posting;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "journal_dimensions")
public class JournalDimension {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "journal_entry_line_id", nullable = false, length = 36)
    private String journalEntryLineId;

    @Column(name = "cost_center_id", length = 36)
    private String costCenterId;

    @Column(name = "project_id", length = 36)
    private String projectId;

    @Column(name = "department_id", length = 36)
    private String departmentId;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected JournalDimension() {
    }

    public JournalDimension(String journalEntryLineId, String costCenterId, String projectId, String departmentId) {
        this.id = UUID.randomUUID().toString();
        this.journalEntryLineId = journalEntryLineId;
        this.costCenterId = costCenterId;
        this.projectId = projectId;
        this.departmentId = departmentId;
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getJournalEntryLineId() {
        return journalEntryLineId;
    }

    public String getCostCenterId() {
        return costCenterId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
