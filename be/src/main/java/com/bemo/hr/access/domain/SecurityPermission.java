package com.bemo.hr.access.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sec_permissions")
public class SecurityPermission {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "permission_key", length = 100, nullable = false, unique = true)
    private String permissionKey;

    @Column(name = "module", length = 50, nullable = false)
    private String module;

    @Column(name = "submodule", length = 50)
    private String submodule;

    @Column(name = "description_key", length = 100)
    private String descriptionKey;

    @Column(name = "action", length = 50)
    private String action;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected SecurityPermission() {
    }

    public SecurityPermission(String permissionKey, String module, String submodule,
                              String descriptionKey, String action, boolean isSystem) {
        this.id = UUID.randomUUID().toString();
        this.permissionKey = Objects.requireNonNull(permissionKey, "permissionKey must not be null").strip();
        this.module = Objects.requireNonNull(module, "module must not be null").strip();
        this.submodule = submodule != null ? submodule.strip() : null;
        this.descriptionKey = descriptionKey != null ? descriptionKey.strip() : null;
        this.action = action != null ? action.strip() : null;
        this.isSystem = isSystem;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getPermissionKey() {
        return permissionKey;
    }

    public String getModule() {
        return module;
    }

    public String getSubmodule() {
        return submodule;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public String getAction() {
        return action;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
