package com.bemo.hr.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_workflow_definitions")
@Getter
public class ApprovalWorkflowDefinition {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "document_type", nullable = false, length = 50) private String documentType;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false) private boolean active;
    @Column(nullable = false) private int version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ApprovalWorkflowDefinition() { }

    public ApprovalWorkflowDefinition(String documentType, String name, boolean active) {
        this.id = UUID.randomUUID().toString();
        this.documentType = documentType.strip().toUpperCase();
        this.name = name.strip();
        this.active = active;
        this.version = 1;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, boolean active) {
        this.name = name.strip();
        this.active = active;
        this.version++;
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
