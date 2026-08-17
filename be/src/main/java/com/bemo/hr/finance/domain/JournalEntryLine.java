package com.bemo.hr.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "journal_entry_lines")
public class JournalEntryLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "journal_entry_id", nullable = false, length = 36)
    private String journalEntryId;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "party_id", length = 36)
    private String partyId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal debit;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal credit;

    @Column(length = 255)
    private String memo;

    protected JournalEntryLine() {
    }

    public JournalEntryLine(String journalEntryId, String accountId, String partyId, BigDecimal debit, BigDecimal credit, String memo) {
        this.id = UUID.randomUUID().toString();
        this.journalEntryId = journalEntryId;
        this.accountId = accountId;
        this.partyId = partyId == null || partyId.isBlank() ? null : partyId.strip();
        this.debit = debit == null ? BigDecimal.ZERO : debit;
        this.credit = credit == null ? BigDecimal.ZERO : credit;
        this.memo = memo == null ? null : memo.strip();
    }

    public String getId() {
        return id;
    }

    public String getJournalEntryId() {
        return journalEntryId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getPartyId() {
        return partyId;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public String getMemo() {
        return memo;
    }
}
