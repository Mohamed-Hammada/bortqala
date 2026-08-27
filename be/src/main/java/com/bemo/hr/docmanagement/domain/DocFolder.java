package com.bemo.hr.docmanagement.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "doc_folders")
public class DocFolder {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "parent_id", length = 100)
    private String parentId;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected DocFolder() {
    }

    public DocFolder(String name, String parentId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.parentId = parentId;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void move(String parentId) {
        this.parentId = parentId;
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getName() { return name; }
    public String getParentId() { return parentId; }
    public long getVersion() { return version; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
