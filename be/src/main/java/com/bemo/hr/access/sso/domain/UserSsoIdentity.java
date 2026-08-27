package com.bemo.hr.access.sso.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "user_sso_identities", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"app_id", "provider", "subject"})
})
public class UserSsoIdentity {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    @Column(nullable = false, length = 20)
    private String provider;
    @Column(nullable = false, length = 500)
    private String subject;
    @Column(name = "email", length = 320)
    private String email;
    @Column(name = "display_name", length = 500)
    private String displayName;
    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected UserSsoIdentity() {}

    public UserSsoIdentity(String appId, String userId, String provider, String subject,
                           String email, String displayName) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.userId = userId;
        this.provider = provider;
        this.subject = subject;
        this.email = email;
        this.displayName = displayName;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getUserId() { return userId; }
    public String getProvider() { return provider; }
    public String getSubject() { return subject; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); }
}
