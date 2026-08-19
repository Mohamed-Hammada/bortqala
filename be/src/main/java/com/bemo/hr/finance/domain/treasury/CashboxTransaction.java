package com.bemo.hr.finance.domain.treasury;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cashbox_transactions")
@Getter
public class CashboxTransaction {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "cashbox_id", nullable = false, length = 36)
    private String cashboxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "voucher_number", length = 50)
    private String voucherNumber;

    @Column(name = "counterparty_party_id", length = 36)
    private String counterpartyPartyId;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "transaction_date", nullable = false)
    private long transactionDate;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected CashboxTransaction() {
    }

    public CashboxTransaction(String cashboxId, TransactionType transactionType, BigDecimal amount,
                              String voucherNumber, String counterpartyPartyId, String description,
                              long transactionDate, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.cashboxId = cashboxId;
        this.transactionType = transactionType;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.voucherNumber = voucherNumber != null ? voucherNumber.strip() : null;
        this.counterpartyPartyId = counterpartyPartyId != null && !counterpartyPartyId.isBlank() ? counterpartyPartyId.strip() : null;
        this.description = description != null ? description.strip() : null;
        this.status = Status.POSTED;
        this.transactionDate = transactionDate > 0 ? transactionDate : System.currentTimeMillis();
        this.createdBy = createdBy;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public enum TransactionType {
        RECEIPT,
        PAYMENT,
        PETTY_CASH_ADVANCE,
        PETTY_CASH_SETTLEMENT,
        PHYSICAL_COUNT_ADJUSTMENT
    }

    public enum Status {
        DRAFT,
        APPROVED,
        POSTED,
        REJECTED,
        CANCELLED
    }
}
