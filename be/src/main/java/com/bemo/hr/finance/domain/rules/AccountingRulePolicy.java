package com.bemo.hr.finance.domain.rules;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "accounting_rule_policies")
public class AccountingRulePolicy {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "policy_code", nullable = false, length = 50)
    private String policyCode;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "trigger_event", nullable = false, length = 50)
    private String triggerEvent;

    @Column(name = "debit_account_pattern", nullable = false, length = 50)
    private String debitAccountPattern;

    @Column(name = "credit_account_pattern", nullable = false, length = 50)
    private String creditAccountPattern;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected AccountingRulePolicy() {}

    public AccountingRulePolicy(String policyCode, String description, String triggerEvent, String debitAccountPattern, String creditAccountPattern) {
        this.id = UUID.randomUUID().toString();
        this.policyCode = policyCode;
        this.description = description;
        this.triggerEvent = triggerEvent;
        this.debitAccountPattern = debitAccountPattern;
        this.creditAccountPattern = creditAccountPattern;
        this.active = true;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getPolicyCode() { return policyCode; }
    public String getDescription() { return description; }
    public String getTriggerEvent() { return triggerEvent; }
    public String getDebitAccountPattern() { return debitAccountPattern; }
    public String getCreditAccountPattern() { return creditAccountPattern; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
