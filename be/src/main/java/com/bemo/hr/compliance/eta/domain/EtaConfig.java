package com.bemo.hr.compliance.eta.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "eta_configs")
public class EtaConfig {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "client_secret", nullable = false, length = 200)
    private String clientSecret;

    @Column(name = "issuer_tax_id", nullable = false, length = 50)
    private String issuerTaxId;

    @Column(name = "issuer_name", nullable = false, length = 200)
    private String issuerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 30)
    private EtaEnvironment environment;

    @Column(name = "token_url", length = 255)
    private String tokenUrl;

    @Column(name = "api_base_url", length = 255)
    private String apiBaseUrl;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected EtaConfig() {
    }

    public EtaConfig(String clientId, String clientSecret, String issuerTaxId, String issuerName, EtaEnvironment environment, String tokenUrl, String apiBaseUrl) {
        this.id = UUID.randomUUID().toString();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.issuerTaxId = issuerTaxId;
        this.issuerName = issuerName;
        this.environment = environment;
        this.tokenUrl = tokenUrl != null ? tokenUrl : (environment == EtaEnvironment.PRODUCTION ? "https://id.eta.gov.eg/connect/token" : "https://id.preprod.eta.gov.eg/connect/token");
        this.apiBaseUrl = apiBaseUrl != null ? apiBaseUrl : (environment == EtaEnvironment.PRODUCTION ? "https://api.invoicing.eta.gov.eg/api/v1.0" : "https://api.preprod.invoicing.eta.gov.eg/api/v1.0");
        this.active = true;
    }

    public void update(String clientId, String clientSecret, String issuerTaxId, String issuerName, EtaEnvironment environment, String tokenUrl, String apiBaseUrl, boolean active) {
        this.clientId = clientId;
        if (clientSecret != null && !clientSecret.isBlank() && !clientSecret.contains("***")) {
            this.clientSecret = clientSecret;
        }
        this.issuerTaxId = issuerTaxId;
        this.issuerName = issuerName;
        this.environment = environment;
        this.tokenUrl = tokenUrl;
        this.apiBaseUrl = apiBaseUrl;
        this.active = active;
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

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getIssuerTaxId() {
        return issuerTaxId;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public EtaEnvironment getEnvironment() {
        return environment;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public boolean isActive() {
        return active;
    }

    public long getVersion() {
        return version;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
