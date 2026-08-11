package com.bemo.hr.manufacturing.production.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "routing_headers")
public class RoutingHeader {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "routing_code", nullable = false, length = 50)
    private String routingCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected RoutingHeader() {}

    public RoutingHeader(String routingCode, String name, String itemId) {
        this.id = UUID.randomUUID().toString();
        this.routingCode = routingCode;
        this.name = name;
        this.itemId = itemId;
        this.active = true;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getRoutingCode() { return routingCode; }
    public String getName() { return name; }
    public String getItemId() { return itemId; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
