package com.bemo.hr.access.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sec_policy_groups")
public class SecurityPolicyGroup {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 50, nullable = false)
    private String appId;

    @Column(name = "group_name", length = 100, nullable = false)
    private String groupName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0L;

    protected SecurityPolicyGroup() {
    }

    public SecurityPolicyGroup(String groupName, String description, boolean isSystem) {
        this.id = UUID.randomUUID().toString();
        this.groupName = Objects.requireNonNull(groupName, "groupName must not be null").strip();
        this.description = description != null ? description.strip() : null;
        this.isSystem = isSystem;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public void update(String groupName, String description) {
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("groupName must not be blank");
        }
        this.groupName = groupName.strip();
        this.description = description != null ? description.strip() : null;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSystem() {
        return isSystem;
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
}
