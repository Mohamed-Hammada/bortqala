package com.bemo.hr.security.pack.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sec_role_ip_allowlists")
public class RoleIpAllowlist {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "role_code", nullable = false)
    private String roleCode;

    @Column(name = "cidr_block", nullable = false)
    private String cidrBlock;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RoleIpAllowlist() {
    }

    public RoleIpAllowlist(String appId, String roleCode, String cidrBlock, String description) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.roleCode = roleCode;
        this.cidrBlock = cidrBlock;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getCidrBlock() {
        return cidrBlock;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
