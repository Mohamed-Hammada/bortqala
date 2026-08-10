package com.bemo.hr.finance.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "bank_reconciliation_matches") @Getter
public class BankReconciliationMatch {
    public enum Type { EXACT, PARTIAL, MANUAL, FEE }
    public enum Status { ACTIVE, REVERSED }
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "statement_line_id", nullable = false, length = 36) private String statementLineId;
    @Column(name = "journal_entry_id", nullable = false, length = 36) private String journalEntryId;
    @Column(name = "matched_amount", nullable = false, precision = 19, scale = 2) private BigDecimal matchedAmount;
    @Enumerated(EnumType.STRING) @Column(name = "match_type", nullable = false, length = 20) private Type matchType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status;
    @Column(name = "operation_id", nullable = false, length = 80) private String operationId;
    @Column(name = "matched_by", nullable = false, length = 100) private String matchedBy;
    @Column(name = "matched_at", nullable = false) private Instant matchedAt;
    @Column(name = "reversed_by", length = 100) private String reversedBy;
    @Column(name = "reversed_at") private Instant reversedAt;
    @Column(name = "reversal_reason", length = 500) private String reversalReason;

    protected BankReconciliationMatch() { }
    public BankReconciliationMatch(String lineId, String journalId, BigDecimal amount, Type type, String operationId, String actor) {
        id = UUID.randomUUID().toString(); statementLineId = lineId; journalEntryId = journalId;
        matchedAmount = amount.abs(); matchType = type; this.operationId = operationId; status = Status.ACTIVE;
        matchedBy = actor; matchedAt = Instant.now();
    }
    public void reverse(String actor, String reason) {
        if (status == Status.REVERSED) return;
        status = Status.REVERSED; reversedBy = actor; reversedAt = Instant.now(); reversalReason = reason.strip();
    }
}
