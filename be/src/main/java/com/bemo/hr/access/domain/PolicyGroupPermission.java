package com.bemo.hr.access.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sec_group_permissions")
public class PolicyGroupPermission {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 50, nullable = false)
    private String appId;

    @Column(name = "policy_group_id", length = 36, nullable = false)
    private String policyGroupId;

    @Column(name = "permission_id", length = 36, nullable = false)
    private String permissionId;

    @Column(name = "granted_at", nullable = false)
    private long grantedAt;

    protected PolicyGroupPermission() {
    }

    public PolicyGroupPermission(String policyGroupId, String permissionId) {
        this.id = UUID.randomUUID().toString();
        this.policyGroupId = Objects.requireNonNull(policyGroupId, "policyGroupId must not be null");
        this.permissionId = Objects.requireNonNull(permissionId, "permissionId must not be null");
        this.grantedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getPolicyGroupId() {
        return policyGroupId;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public long getGrantedAt() {
        return grantedAt;
    }
}
