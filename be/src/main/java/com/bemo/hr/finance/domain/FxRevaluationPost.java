package com.bemo.hr.finance.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fx_revaluation_posts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"app_id", "currency_code", "year_month"})
})
public class FxRevaluationPost {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "total_unrealized_gain", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalUnrealizedGain;

    @Column(name = "total_unrealized_loss", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalUnrealizedLoss;

    @Column(name = "journal_entry_id", nullable = false, length = 36)
    private String journalEntryId;

    @Column(name = "posted_by", nullable = false, length = 100)
    private String postedBy;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected FxRevaluationPost() {
    }

    public FxRevaluationPost(String currencyCode, String yearMonth,
                             BigDecimal totalUnrealizedGain, BigDecimal totalUnrealizedLoss,
                             String journalEntryId, String postedBy) {
        this.id = UUID.randomUUID().toString();
        this.currencyCode = currencyCode.strip().toUpperCase();
        this.yearMonth = yearMonth.strip();
        this.totalUnrealizedGain = totalUnrealizedGain == null ? BigDecimal.ZERO : totalUnrealizedGain;
        this.totalUnrealizedLoss = totalUnrealizedLoss == null ? BigDecimal.ZERO : totalUnrealizedLoss;
        this.journalEntryId = journalEntryId;
        this.postedBy = postedBy;
        this.postedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getCurrencyCode() { return currencyCode; }
    public String getYearMonth() { return yearMonth; }
    public BigDecimal getTotalUnrealizedGain() { return totalUnrealizedGain; }
    public BigDecimal getTotalUnrealizedLoss() { return totalUnrealizedLoss; }
    public String getJournalEntryId() { return journalEntryId; }
    public String getPostedBy() { return postedBy; }
    public Instant getPostedAt() { return postedAt; }
    public long getCreatedAt() { return createdAt; }
}
