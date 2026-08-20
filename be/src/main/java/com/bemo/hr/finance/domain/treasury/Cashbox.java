package com.bemo.hr.finance.domain.treasury;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cashboxes")
@Getter
@FilterDefs({
    @FilterDef(name = "orgScopeFilter", parameters = {
        @ParamDef(name = "scopeLevel", type = String.class),
        @ParamDef(name = "userBranchId", type = String.class),
        @ParamDef(name = "userDepartmentId", type = String.class),
        @ParamDef(name = "userId", type = String.class)
    })
})
@Filter(name = "orgScopeFilter", condition =
    "(:scopeLevel = 'GLOBAL' OR " +
    "(:scopeLevel = 'BRANCH' AND branch_id = :userBranchId) OR " +
    "(:scopeLevel = 'SELF' AND custodian_user_id = :userId))")
public class Cashbox {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "branch_id", length = 36)
    private String branchId;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "custodian_user_id", length = 100)
    private String custodianUserId;

    @Column(name = "gl_account_id", length = 36)
    private String glAccountId;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Cashbox() {
    }

    public Cashbox(String code, String name, String branchId, String currency, String custodianUserId, String glAccountId) {
        this.id = UUID.randomUUID().toString();
        this.code = code != null ? code.strip() : "";
        this.name = name != null ? name.strip() : "";
        this.branchId = branchId != null && !branchId.isBlank() ? branchId.strip() : null;
        this.currency = currency != null && !currency.isBlank() ? currency.strip().toUpperCase() : "EGP";
        this.custodianUserId = custodianUserId != null && !custodianUserId.isBlank() ? custodianUserId.strip() : null;
        this.glAccountId = glAccountId != null && !glAccountId.isBlank() ? glAccountId.strip() : null;
        this.currentBalance = BigDecimal.ZERO;
        this.active = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public void adjustBalance(BigDecimal delta) {
        if (delta != null) {
            this.currentBalance = this.currentBalance.add(delta);
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public void updateDetails(String name, String branchId, String custodianUserId, String glAccountId, boolean active) {
        if (name != null && !name.isBlank()) {
            this.name = name.strip();
        }
        this.branchId = branchId != null && !branchId.isBlank() ? branchId.strip() : null;
        this.custodianUserId = custodianUserId != null && !custodianUserId.isBlank() ? custodianUserId.strip() : null;
        this.glAccountId = glAccountId != null && !glAccountId.isBlank() ? glAccountId.strip() : null;
        this.active = active;
        this.updatedAt = System.currentTimeMillis();
    }
}
