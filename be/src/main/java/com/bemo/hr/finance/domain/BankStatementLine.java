package com.bemo.hr.finance.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "bank_statement_lines") @Getter
public class BankStatementLine {
    public enum Status { UNMATCHED, PARTIAL, MATCHED, IGNORED }
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "statement_id", nullable = false, length = 36) private String statementId;
    @Column(name = "line_number", nullable = false) private int lineNumber;
    @Column(name = "transaction_date", nullable = false) private LocalDate transactionDate;
    @Column(name = "value_date") private LocalDate valueDate;
    @Column(nullable = false, length = 1000) private String description;
    @Column(name = "bank_reference", length = 200) private String bankReference;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(name = "running_balance", precision = 19, scale = 2) private BigDecimal runningBalance;
    @Column(nullable = false, length = 64) private String fingerprint;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status;
    @Column(name = "matched_amount", nullable = false, precision = 19, scale = 2) private BigDecimal matchedAmount;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected BankStatementLine() { }
    public BankStatementLine(String statementId, int lineNumber, LocalDate date, LocalDate valueDate,
                             String description, String reference, BigDecimal amount, BigDecimal balance, String fingerprint) {
        id = UUID.randomUUID().toString(); this.statementId = statementId; this.lineNumber = lineNumber;
        transactionDate = date; this.valueDate = valueDate; this.description = description.strip();
        bankReference = reference == null || reference.isBlank() ? null : reference.strip();
        this.amount = amount; runningBalance = balance; this.fingerprint = fingerprint;
        status = Status.UNMATCHED; matchedAmount = BigDecimal.ZERO; createdAt = Instant.now();
    }
    public BigDecimal remainingAmount() { return amount.abs().subtract(matchedAmount); }
    public void addMatch(BigDecimal value) { matchedAmount = matchedAmount.add(value.abs()); status = remainingAmount().signum() == 0 ? Status.MATCHED : Status.PARTIAL; }
    public void reverseMatch(BigDecimal value) { matchedAmount = matchedAmount.subtract(value.abs()).max(BigDecimal.ZERO); status = matchedAmount.signum() == 0 ? Status.UNMATCHED : Status.PARTIAL; }
    public void ignore() { status = Status.IGNORED; }
}
