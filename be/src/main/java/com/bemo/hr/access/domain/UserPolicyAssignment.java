package com.bemo.hr.access.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sec_user_policy_assignments")
public class UserPolicyAssignment {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 50, nullable = false)
    private String appId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "policy_group_id", length = 36, nullable = false)
    private String policyGroupId;

    @Column(name = "scope_branch_id", length = 50)
    private String scopeBranchId;

    @Column(name = "scope_cost_center_id", length = 50)
    private String scopeCostCenterId;

    @Column(name = "assigned_at", nullable = false)
    private long assignedAt;

    protected UserPolicyAssignment() {
    }

    public UserPolicyAssignment(String userId, String policyGroupId, String scopeBranchId, String scopeCostCenterId) {
        this.id = UUID.randomUUID().toString();
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.policyGroupId = Objects.requireNonNull(policyGroupId, "policyGroupId must not be null");
        this.scopeBranchId = scopeBranchId != null && !scopeBranchId.isBlank() ? scopeBranchId.strip() : null;
        this.scopeCostCenterId = scopeCostCenterId != null && !scopeCostCenterId.isBlank() ? scopeCostCenterId.strip() : null;
        this.assignedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getUserId() {
        return userId;
    }

    public String getPolicyGroupId() {
        return policyGroupId;
    }

    public String getScopeBranchId() {
        return scopeBranchId;
    }

    public String getScopeCostCenterId() {
        return scopeCostCenterId;
    }

    public long getAssignedAt() {
        return assignedAt;
    }
}
