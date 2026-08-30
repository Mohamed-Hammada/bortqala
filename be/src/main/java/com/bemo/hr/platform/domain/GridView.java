package com.bemo.hr.platform.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "grid_views")
public class GridView {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;
    @Column(name = "page_key", nullable = false, length = 100)
    private String pageKey;
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Column(name = "filters", columnDefinition = "text")
    private String filters;
    @Column(name = "hidden_columns", length = 1000)
    private String hiddenColumns;
    @Column(name = "sort", length = 200)
    private String sort;
    @Column(name = "shared_roles", length = 200)
    private String sharedRoles;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;

    protected GridView() {}

    public GridView(String appId, String userId, String pageKey, String name,
                    String filters, String hiddenColumns, String sort) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.userId = userId;
        this.pageKey = pageKey;
        this.name = name;
        this.filters = filters;
        this.hiddenColumns = hiddenColumns;
        this.sort = sort;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getUserId() { return userId; }
    public String getPageKey() { return pageKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFilters() { return filters; }
    public void setFilters(String filters) { this.filters = filters; }
    public String getHiddenColumns() { return hiddenColumns; }
    public void setHiddenColumns(String hiddenColumns) { this.hiddenColumns = hiddenColumns; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public String getSharedRoles() { return sharedRoles; }
    public void setSharedRoles(String sharedRoles) { this.sharedRoles = sharedRoles; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    @PrePersist
    void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
