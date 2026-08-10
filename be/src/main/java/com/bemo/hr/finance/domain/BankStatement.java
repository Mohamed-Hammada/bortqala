package com.bemo.hr.finance.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "bank_statements") @Getter
public class BankStatement {
    public enum Status { IMPORTED, IN_PROGRESS, RECONCILED }
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "bank_account_id", nullable = false, length = 36) private String bankAccountId;
    @Column(name = "statement_reference", nullable = false, length = 100) private String statementReference;
    @Column(name = "period_start", nullable = false) private LocalDate periodStart;
    @Column(name = "period_end", nullable = false) private LocalDate periodEnd;
    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 2) private BigDecimal openingBalance;
    @Column(name = "closing_balance", nullable = false, precision = 19, scale = 2) private BigDecimal closingBalance;
    @Column(name = "currency_code", nullable = false, length = 10) private String currencyCode;
    @Column(name = "file_name", nullable = false, length = 255) private String fileName;
    @Column(name = "file_hash", nullable = false, length = 64) private String fileHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status;
    @Column(name = "imported_by", nullable = false, length = 100) private String importedBy;
    @Column(name = "imported_at", nullable = false) private Instant importedAt;
    @Column(name = "reconciled_by", length = 100) private String reconciledBy;
    @Column(name = "reconciled_at") private Instant reconciledAt;
    @Version private long version;

    protected BankStatement() { }
    public BankStatement(String bankAccountId, String reference, LocalDate start, LocalDate end,
                         BigDecimal opening, BigDecimal closing, String currency, String fileName,
                         String fileHash, String actor) {
        id = UUID.randomUUID().toString(); this.bankAccountId = bankAccountId; statementReference = reference.strip();
        periodStart = start; periodEnd = end; openingBalance = opening; closingBalance = closing;
        currencyCode = currency.strip().toUpperCase(); this.fileName = fileName; this.fileHash = fileHash;
        status = Status.IMPORTED; importedBy = actor; importedAt = Instant.now();
    }
    public void updateProgress(long unmatched, String actor) {
        if (unmatched == 0) { status = Status.RECONCILED; reconciledBy = actor; reconciledAt = Instant.now(); }
        else { status = Status.IN_PROGRESS; reconciledBy = null; reconciledAt = null; }
    }
}
