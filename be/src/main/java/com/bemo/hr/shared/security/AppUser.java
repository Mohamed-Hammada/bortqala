package com.bemo.hr.shared.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    private String id;

    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_code"))
    private Set<Role> roles = new LinkedHashSet<>();

    @Column(name = "allowed_menus", length = 1000)
    private String allowedMenus;

    @Column(name = "can_view_salary", nullable = false)
    private boolean canViewSalary = true;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {
    }

    public AppUser(String appId, String username, String displayName, String passwordHash, Set<Role> roles, Set<String> allowedMenus, Boolean canViewSalary) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        update(username, displayName, passwordHash, true, roles, allowedMenus, canViewSalary);
    }

    public void update(String username, String displayName, String passwordHash, boolean active, Set<Role> roles, Set<String> allowedMenus, Boolean canViewSalary) {
        this.username = username.strip().toLowerCase();
        this.displayName = displayName.strip();
        if (passwordHash != null) this.passwordHash = passwordHash;
        this.active = active;
        this.roles.clear();
        this.roles.addAll(roles);
        this.canViewSalary = canViewSalary == null ? true : canViewSalary;
        if (allowedMenus != null && !allowedMenus.isEmpty()) {
            this.allowedMenus = String.join(",", allowedMenus);
        } else {
            this.allowedMenus = "dashboard,employees,categories,reports,imports,parties,operations,users,settings";
        }
    }

    @PrePersist
    void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
    public boolean isCanViewSalary() { return canViewSalary; }
    public Set<Role> getRoles() { return Set.copyOf(roles); }
    public Set<String> getAllowedMenus() {
        if (allowedMenus == null || allowedMenus.isBlank()) {
            return Set.of("dashboard","employees","categories","reports","imports","parties","operations","users","settings");
        }
        return Set.of(allowedMenus.split(","));
    }
    public long getVersion() { return version; }
}
