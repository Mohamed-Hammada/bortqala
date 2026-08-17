package com.bemo.hr.shared.shortcut.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_screen_shortcuts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_shortcut_profile_key",
                        columnNames = {"app_id", "profile_id", "second_key_code"}
                ),
                @UniqueConstraint(
                        name = "uk_user_shortcut_profile_page",
                        columnNames = {"app_id", "profile_id", "page_code"}
                )
        }
)
@Getter
public class UserScreenShortcut {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "profile_id", nullable = false)
    private String profileId;

    @Column(name = "page_code", nullable = false, length = 80)
    private String pageCode;

    @Column(name = "second_key_code", nullable = false, length = 20)
    private String secondKeyCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserScreenShortcut() {
    }

    public UserScreenShortcut(
            String profileId,
            String pageCode,
            String secondKeyCode,
            boolean enabled,
            int sortOrder
    ) {
        this.id = UUID.randomUUID().toString();
        this.profileId = profileId;
        this.pageCode = pageCode;
        this.secondKeyCode = secondKeyCode;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
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
