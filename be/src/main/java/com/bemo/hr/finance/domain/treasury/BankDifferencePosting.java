package com.bemo.hr.finance.domain.treasury;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bank_difference_postings")
public class BankDifferencePosting {

    public enum DifferenceType {
        FEE, INTEREST, UNEXPLAINED
    }

    public enum Status {
        DRAFT, POSTED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "statement_line_id", nullable = false, length = 36)
    private String statementLineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "difference_type", nullable = false, length = 20)
    private DifferenceType differenceType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.POSTED;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected BankDifferencePosting() {}

    public BankDifferencePosting(String statementLineId, DifferenceType differenceType, BigDecimal amount) {
        this.id = UUID.randomUUID().toString();
        this.statementLineId = statementLineId;
        this.differenceType = differenceType;
        this.amount = amount;
        this.status = Status.POSTED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getStatementLineId() { return statementLineId; }
    public DifferenceType getDifferenceType() { return differenceType; }
    public BigDecimal getAmount() { return amount; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
