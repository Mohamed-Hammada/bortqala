package com.bemo.hr.finance.domain.treasury;

import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "commercial_cheques")
@Getter
public class CommercialCheque {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "cheque_number", nullable = false, length = 50)
    private String chequeNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "cheque_type", nullable = false, length = 20)
    private ChequeType chequeType;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_id", length = 36)
    private String bankAccountId;

    @Column(name = "drawer_payee_name", nullable = false, length = 150)
    private String drawerPayeeName;

    @Column(name = "party_id", length = 36)
    private String partyId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "issue_date", nullable = false)
    private long issueDate;

    @Column(name = "due_date", nullable = false)
    private long dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "bounce_reason", length = 255)
    private String bounceReason;

    @Column(length = 500)
    private String notes;

    @Column(name = "journal_entry_id", length = 36)
    private String journalEntryId;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected CommercialCheque() {
    }

    public CommercialCheque(String chequeNumber, ChequeType chequeType, String bankName, String bankAccountId,
                            String drawerPayeeName, String partyId, BigDecimal amount, String currency,
                            long issueDate, long dueDate, String notes) {
        this.id = UUID.randomUUID().toString();
        this.chequeNumber = chequeNumber != null ? chequeNumber.strip() : "";
        this.chequeType = chequeType != null ? chequeType : ChequeType.RECEIVED;
        this.bankName = bankName != null ? bankName.strip() : null;
        this.bankAccountId = bankAccountId != null && !bankAccountId.isBlank() ? bankAccountId.strip() : null;
        this.drawerPayeeName = drawerPayeeName != null ? drawerPayeeName.strip() : "";
        this.partyId = partyId != null && !partyId.isBlank() ? partyId.strip() : null;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.currency = currency != null && !currency.isBlank() ? currency.strip().toUpperCase() : "EGP";
        this.issueDate = issueDate > 0 ? issueDate : System.currentTimeMillis();
        this.dueDate = dueDate > 0 ? dueDate : this.issueDate;
        this.status = this.chequeType == ChequeType.ISSUED ? Status.ISSUED : Status.RECEIVED;
        this.notes = notes != null ? notes.strip() : null;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public void deposit(String targetBankAccountId) {
        if (this.status != Status.RECEIVED && this.status != Status.BOUNCED) {
            throw new BusinessRuleException("Only received or bounced cheques can be deposited", "INVALID_CHEQUE_STATE", HttpStatus.CONFLICT);
        }
        this.bankAccountId = targetBankAccountId;
        this.status = Status.DEPOSITED;
        this.updatedAt = System.currentTimeMillis();
    }

    public void collect() {
        if (this.status != Status.DEPOSITED && this.status != Status.ISSUED) {
            throw new BusinessRuleException("Only deposited received cheques or issued cheques can be collected/cleared", "INVALID_CHEQUE_STATE", HttpStatus.CONFLICT);
        }
        this.status = Status.COLLECTED;
        this.updatedAt = System.currentTimeMillis();
    }

    public void bounce(String reason) {
        if (this.status != Status.DEPOSITED && this.status != Status.ISSUED) {
            throw new BusinessRuleException("Only deposited or issued cheques can bounce", "INVALID_CHEQUE_STATE", HttpStatus.CONFLICT);
        }
        this.status = Status.BOUNCED;
        this.bounceReason = reason != null ? reason.strip() : "Insufficient funds / Technical reason";
        this.updatedAt = System.currentTimeMillis();
    }

    public void cancel(String reason) {
        if (this.status == Status.COLLECTED) {
            throw new BusinessRuleException("Collected cheques cannot be cancelled", "INVALID_CHEQUE_STATE", HttpStatus.CONFLICT);
        }
        this.status = Status.CANCELLED;
        this.notes = (this.notes != null ? this.notes + " | " : "") + (reason != null ? reason.strip() : "Cancelled");
        this.updatedAt = System.currentTimeMillis();
    }

    public enum ChequeType {
        RECEIVED,
        ISSUED
    }

    public enum Status {
        RECEIVED,
        ISSUED,
        DEPOSITED,
        COLLECTED,
        BOUNCED,
        CANCELLED
    }
}
