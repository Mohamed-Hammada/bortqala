package com.bemo.hr.shared.shortcut.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_shortcut_profiles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_shortcut_profile_app_user",
                columnNames = {"app_id", "user_id"}
        )
)
@Getter
public class UserShortcutProfile {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_mode", nullable = false, length = 20)
    private ShortcutProfileMode profileMode;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserShortcutProfile() {
    }

    public UserShortcutProfile(String userId) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.profileMode = ShortcutProfileMode.DEFAULT;
    }

    public void useCustomProfile() {
        this.profileMode = ShortcutProfileMode.CUSTOM;
    }

    public void resetToDefault() {
        this.profileMode = ShortcutProfileMode.DEFAULT;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
