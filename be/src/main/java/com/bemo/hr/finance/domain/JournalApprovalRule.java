package com.bemo.hr.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "journal_approval_rules")
public class JournalApprovalRule {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "max_amount_without_approval", nullable = false, precision = 15, scale = 2)
    private BigDecimal maxAmountWithoutApproval;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected JournalApprovalRule() {}

    public JournalApprovalRule(String accountId, BigDecimal maxAmountWithoutApproval, boolean requiresApproval) {
        this.id = UUID.randomUUID().toString();
        this.accountId = accountId;
        this.maxAmountWithoutApproval = maxAmountWithoutApproval;
        this.requiresApproval = requiresApproval;
    }

    public void update(BigDecimal maxAmountWithoutApproval, boolean requiresApproval) {
        this.maxAmountWithoutApproval = maxAmountWithoutApproval;
        this.requiresApproval = requiresApproval;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getAccountId() { return accountId; }
    public BigDecimal getMaxAmountWithoutApproval() { return maxAmountWithoutApproval; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
