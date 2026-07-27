package com.bemo.hr.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    public enum Type {
        ASSET,
        LIABILITY,
        EQUITY,
        REVENUE,
        EXPENSE
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    @Column(name = "parent_id", length = 36)
    private String parentId;

    @Column(name = "is_header", nullable = false)
    private boolean isHeader;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected Account() {}

    public Account(String code, String name, Type type, String parentId, boolean isHeader, String currency, boolean active) {
        this.id = UUID.randomUUID().toString();
        update(code, name, type, parentId, isHeader, currency, active);
    }

    public void update(String code, String name, Type type, String parentId, boolean isHeader, String currency, boolean active) {
        this.code = code.strip();
        this.name = name.strip();
        this.type = type;
        this.parentId = parentId == null || parentId.isBlank() ? null : parentId.strip();
        this.isHeader = isHeader;
        this.currency = currency == null || currency.isBlank() ? "EGP" : currency.strip().toUpperCase();
        this.active = active;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Type getType() { return type; }
    public String getParentId() { return parentId; }
    public boolean isHeader() { return isHeader; }
    public String getCurrency() { return currency; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
