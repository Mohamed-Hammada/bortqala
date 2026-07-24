package com.bemo.hr.shared.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "apps")
public class TenantApplication {
    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "session_timeout_minutes", nullable = false)
    private int sessionTimeoutMinutes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TenantApplication() { }

    public TenantApplication(String code, String name) {
        this.id = UUID.randomUUID().toString();
        this.code = code.strip().toUpperCase(Locale.ROOT);
        this.name = name.strip();
        this.active = true;
        this.sessionTimeoutMinutes = 480;
    }

    public void updateSessionTimeoutMinutes(int sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    @PrePersist
    void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public Instant getUpdatedAt() { return updatedAt; }
}
