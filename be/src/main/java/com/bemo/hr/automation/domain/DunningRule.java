package com.bemo.hr.automation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dunning_rules")
public class DunningRule {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "days_overdue", nullable = false)
    private int daysOverdue;
    @Column(name = "template_key", nullable = false, length = 100)
    private String templateKey;
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    private Long version;

    protected DunningRule() {}

    public DunningRule(String appId, int daysOverdue, String templateKey, String channel) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.daysOverdue = daysOverdue;
        this.templateKey = templateKey;
        this.channel = channel;
        this.active = true;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public int getDaysOverdue() { return daysOverdue; }
    public String getTemplateKey() { return templateKey; }
    public String getChannel() { return channel; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Long getVersion() { return version; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }
}
