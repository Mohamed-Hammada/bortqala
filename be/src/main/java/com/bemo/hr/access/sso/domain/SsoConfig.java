package com.bemo.hr.access.sso.domain;

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
@Table(name = "sso_configs")
public class SsoConfig {

    public enum Provider { GOOGLE, MICROSOFT }

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(nullable = false, length = 20)
    private String provider;
    @Column(name = "client_id", nullable = false, length = 500)
    private String clientId;
    @Column(nullable = false, length = 1000)
    private String secret;
    @Column(length = 500)
    private String issuer;
    @Column(name = "discovery_url", length = 1000)
    private String discoveryUrl;
    @Column(name = "auto_provision", nullable = false)
    private boolean autoProvision;
    @Column(name = "default_role", length = 50)
    private String defaultRole;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    private Long version;

    protected SsoConfig() {}

    public SsoConfig(String appId, Provider provider, String clientId, String secret,
                     String issuer, String discoveryUrl, boolean autoProvision, String defaultRole) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.provider = provider.name();
        this.clientId = clientId;
        this.secret = secret;
        this.issuer = issuer;
        this.discoveryUrl = discoveryUrl;
        this.autoProvision = autoProvision;
        this.defaultRole = defaultRole != null ? defaultRole : "VIEWER";
        this.active = true;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public Provider getProvider() { return Provider.valueOf(provider); }
    public String getClientId() { return clientId; }
    public String getSecret() { return secret; }
    public String getIssuer() { return issuer; }
    public String getDiscoveryUrl() { return discoveryUrl; }
    public boolean isAutoProvision() { return autoProvision; }
    public String getDefaultRole() { return defaultRole; }
    public boolean isActive() { return active; }
    public Long getVersion() { return version; }

    public void setClientId(String clientId) { this.clientId = clientId; }
    public void setSecret(String secret) { this.secret = secret; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public void setDiscoveryUrl(String discoveryUrl) { this.discoveryUrl = discoveryUrl; }
    public void setAutoProvision(boolean autoProvision) { this.autoProvision = autoProvision; }
    public void setDefaultRole(String defaultRole) { this.defaultRole = defaultRole; }
    public void setActive(boolean active) { this.active = active; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }
}
