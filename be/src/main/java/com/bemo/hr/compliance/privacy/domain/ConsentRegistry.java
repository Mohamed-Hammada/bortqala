package com.bemo.hr.compliance.privacy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consent_registry")
public class ConsentRegistry {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "subject_ref", nullable = false, length = 36)
    private String subjectRef;
    @Column(name = "subject_type", nullable = false, length = 20)
    private String subjectType;
    @Column(name = "purpose_key", nullable = false, length = 100)
    private String purposeKey;
    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;
    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;
    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected ConsentRegistry() {}

    public ConsentRegistry(String appId, String subjectRef, String subjectType, String purposeKey) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.subjectRef = subjectRef;
        this.subjectType = subjectType;
        this.purposeKey = purposeKey;
        this.grantedAt = Instant.now();
    }

    public void withdraw() { this.withdrawnAt = Instant.now(); }
    public boolean isActive() { return withdrawnAt == null; }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getSubjectRef() { return subjectRef; }
    public String getSubjectType() { return subjectType; }
    public String getPurposeKey() { return purposeKey; }
    public Instant getGrantedAt() { return grantedAt; }
    public Instant getWithdrawnAt() { return withdrawnAt; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); }
}
