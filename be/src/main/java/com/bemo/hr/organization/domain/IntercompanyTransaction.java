package com.bemo.hr.organization.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "intercompany_transactions")
public class IntercompanyTransaction {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "transaction_number", nullable = false, length = 50)
    private String transactionNumber;

    @Column(name = "from_company_id", nullable = false, length = 36)
    private String fromCompanyId;

    @Column(name = "from_branch_id", length = 36)
    private String fromBranchId;

    @Column(name = "to_company_id", nullable = false, length = 36)
    private String toCompanyId;

    @Column(name = "to_branch_id", length = 36)
    private String toBranchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private IntercompanyType transactionType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "due_to_account_id", length = 36)
    private String dueToAccountId;

    @Column(name = "due_from_account_id", length = 36)
    private String dueFromAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private IntercompanyStatus status;

    @Column(name = "eliminated_in_period", length = 50)
    private String eliminatedInPeriod;

    @Column(name = "journal_entry_id", length = 36)
    private String journalEntryId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected IntercompanyTransaction() {
    }

    public IntercompanyTransaction(
            String transactionNumber,
            String fromCompanyId,
            String fromBranchId,
            String toCompanyId,
            String toBranchId,
            IntercompanyType transactionType,
            BigDecimal amount,
            String currency,
            String description,
            String dueToAccountId,
            String dueFromAccountId
    ) {
        if (fromCompanyId.equals(toCompanyId)) {
            throw new BusinessRuleException("Originating and destination companies must be different", "IC_SAME_COMPANY", HttpStatus.BAD_REQUEST);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Intercompany transaction amount must be positive", "IC_INVALID_AMOUNT", HttpStatus.BAD_REQUEST);
        }
        this.id = UUID.randomUUID().toString();
        this.transactionNumber = transactionNumber;
        this.fromCompanyId = fromCompanyId;
        this.fromBranchId = fromBranchId;
        this.toCompanyId = toCompanyId;
        this.toBranchId = toBranchId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.currency = currency == null || currency.isBlank() ? "EGP" : currency.strip();
        this.description = description == null ? null : description.strip();
        this.dueToAccountId = dueToAccountId;
        this.dueFromAccountId = dueFromAccountId;
        this.status = IntercompanyStatus.PENDING_APPROVAL;
    }

    public void approve() {
        if (this.status != IntercompanyStatus.PENDING_APPROVAL && this.status != IntercompanyStatus.DRAFT) {
            throw new BusinessRuleException("Only draft or pending transactions can be approved", "IC_INVALID_STATUS_TRANSITION", HttpStatus.BAD_REQUEST);
        }
        this.status = IntercompanyStatus.APPROVED;
    }

    public void settle() {
        if (this.status != IntercompanyStatus.APPROVED) {
            throw new BusinessRuleException("Only approved transactions can be settled", "IC_INVALID_STATUS_TRANSITION", HttpStatus.BAD_REQUEST);
        }
        this.status = IntercompanyStatus.SETTLED;
    }

    public void eliminate(String period) {
        if (this.status != IntercompanyStatus.APPROVED && this.status != IntercompanyStatus.SETTLED) {
            throw new BusinessRuleException("Only approved or settled transactions can be eliminated", "IC_INVALID_STATUS_TRANSITION", HttpStatus.BAD_REQUEST);
        }
        this.status = IntercompanyStatus.ELIMINATED;
        this.eliminatedInPeriod = period;
    }

    public void linkJournalEntry(String journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == 0) {
            createdAt = System.currentTimeMillis();
        }
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getTransactionNumber() {
        return transactionNumber;
    }

    public String getFromCompanyId() {
        return fromCompanyId;
    }

    public String getFromBranchId() {
        return fromBranchId;
    }

    public String getToCompanyId() {
        return toCompanyId;
    }

    public String getToBranchId() {
        return toBranchId;
    }

    public IntercompanyType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public String getDueToAccountId() {
        return dueToAccountId;
    }

    public String getDueFromAccountId() {
        return dueFromAccountId;
    }

    public IntercompanyStatus getStatus() {
        return status;
    }

    public String getEliminatedInPeriod() {
        return eliminatedInPeriod;
    }

    public String getJournalEntryId() {
        return journalEntryId;
    }

    public Long getVersion() {
        return version;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
