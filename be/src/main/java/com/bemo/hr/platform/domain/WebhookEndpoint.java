package com.bemo.hr.platform.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_endpoints")
public class WebhookEndpoint {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "url", nullable = false, length = 500)
    private String url;
    @Column(name = "secret", nullable = false, length = 128)
    private String secret;
    @Column(name = "events", nullable = false, length = 1000)
    private String events;
    @Column(name = "active", nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;

    protected WebhookEndpoint() {}

    public WebhookEndpoint(String appId, String url, String secret, String events) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.url = url;
        this.secret = secret;
        this.events = events;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSecret() { return secret; }
    public String getEvents() { return events; }
    public void setEvents(String events) { this.events = events; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public java.util.Set<String> eventSet() {
        if (events == null || events.isBlank()) return java.util.Set.of();
        return java.util.Set.of(events.split(","));
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
