package com.bemo.hr.finance.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "cost_centers")
@Getter
public class CostCenter {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "parent_id", length = 36)
    private String parentId;

    @Column(name = "manager_user_id", length = 100)
    private String managerUserId;

    @Column(name = "is_header", nullable = false)
    private boolean isHeader;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "effective_start_date")
    private Long effectiveStartDate;

    @Column(name = "effective_end_date")
    private Long effectiveEndDate;

    @Column(name = "gl_allocation_rule", length = 255)
    private String glAllocationRule;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    private long version;

    protected CostCenter() {
    }

    public CostCenter(String code, String name, String parentId, String managerUserId,
                      boolean isHeader, boolean active, Long effectiveStartDate,
                      Long effectiveEndDate, String glAllocationRule) {
        this.id = UUID.randomUUID().toString();
        update(code, name, parentId, managerUserId, isHeader, active, effectiveStartDate, effectiveEndDate, glAllocationRule);
    }

    public void update(String code, String name, String parentId, String managerUserId,
                       boolean isHeader, boolean active, Long effectiveStartDate,
                       Long effectiveEndDate, String glAllocationRule) {
        this.code = code != null ? code.strip() : "";
        this.name = name != null ? name.strip() : "";
        this.parentId = parentId != null && !parentId.isBlank() ? parentId.strip() : null;
        this.managerUserId = managerUserId != null && !managerUserId.isBlank() ? managerUserId.strip() : null;
        this.isHeader = isHeader;
        this.active = active;
        this.effectiveStartDate = effectiveStartDate;
        this.effectiveEndDate = effectiveEndDate;
        this.glAllocationRule = glAllocationRule != null && !glAllocationRule.isBlank() ? glAllocationRule.strip() : null;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }
}
