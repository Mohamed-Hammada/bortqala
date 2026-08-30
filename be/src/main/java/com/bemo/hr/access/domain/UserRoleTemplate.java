package com.bemo.hr.access.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.util.Objects;
import java.util.UUID;

/**
 * A job role template for a business vertical (WP-10).
 *
 * <p>Rows with a null {@code appId} are global seeds shipped with the product;
 * rows carrying an app id are tenant-owned customizations that shadow the
 * global row with the same {@code vertical} + {@code code}. The table is
 * deliberately not registered with the Hibernate {@link TenantId} filter so a
 * single query can merge both scopes; scoping is enforced by
 * {@code UserRoleTemplateService}.</p>
 */
@Entity
@Table(name = "user_role_templates")
public class UserRoleTemplate {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "app_id", length = 50)
    private String appId;

    @Column(name = "vertical", length = 20, nullable = false)
    private String vertical;

    @Column(name = "code", length = 60, nullable = false)
    private String code;

    @Column(name = "name_key", length = 120, nullable = false)
    private String nameKey;

    /** Comma-separated canonical menu ids (AccessCatalog menuId values). */
    @Column(name = "menu_ids", length = 2000, nullable = false)
    private String menuIds;

    /** Comma-separated permission prefixes used to suggest matching policy groups. */
    @Column(name = "permission_prefixes", length = 1000, nullable = false)
    private String permissionPrefixes;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0L;

    protected UserRoleTemplate() {
    }

    public UserRoleTemplate(String appId, String vertical, String code, String nameKey,
                            String menuIds, String permissionPrefixes, int sortOrder) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.vertical = Objects.requireNonNull(vertical, "vertical must not be null").strip();
        this.code = Objects.requireNonNull(code, "code must not be null").strip();
        this.nameKey = Objects.requireNonNull(nameKey, "nameKey must not be null").strip();
        this.menuIds = Objects.requireNonNull(menuIds, "menuIds must not be null").strip();
        this.permissionPrefixes = Objects.requireNonNull(permissionPrefixes, "permissionPrefixes must not be null").strip();
        this.sortOrder = sortOrder;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getVertical() {
        return vertical;
    }

    public String getCode() {
        return code;
    }

    public String getNameKey() {
        return nameKey;
    }

    public String getMenuIds() {
        return menuIds;
    }

    public String getPermissionPrefixes() {
        return permissionPrefixes;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
