package com.bemo.hr.organization.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "departments")
public class Department {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "company_id", nullable = false, length = 36)
    private String companyId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "manager_id", length = 36)
    private String managerId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected Department() {
    }

    public Department(String companyId, String code, String name, String managerId, boolean active) {
        this.id = UUID.randomUUID().toString();
        update(companyId, code, name, managerId, active);
    }

    public void update(String companyId, String code, String name, String managerId, boolean active) {
        this.companyId = companyId;
        this.code = code.strip();
        this.name = name.strip();
        this.managerId = managerId == null || managerId.isBlank() ? null : managerId.strip();
        this.active = active;
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

    public String getCompanyId() {
        return companyId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getManagerId() {
        return managerId;
    }

    public boolean isActive() {
        return active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
