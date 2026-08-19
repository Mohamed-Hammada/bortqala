package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "project_party_roles")
public class ProjectPartyRole {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "party_id", nullable = false, length = 36)
    private String partyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 40)
    private ProjectPartyRoleType roleType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected ProjectPartyRole() {
    }

    public ProjectPartyRole(String projectId, String partyId, ProjectPartyRoleType roleType, String notes) {
        this.id = UUID.randomUUID().toString();
        this.projectId = projectId;
        this.partyId = partyId;
        this.roleType = roleType;
        this.notes = notes != null && !notes.isBlank() ? notes.strip() : null;
    }

    public void update(ProjectPartyRoleType roleType, String notes) {
        this.roleType = roleType;
        this.notes = notes != null && !notes.isBlank() ? notes.strip() : null;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getPartyId() {
        return partyId;
    }

    public ProjectPartyRoleType getRoleType() {
        return roleType;
    }

    public String getNotes() {
        return notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
