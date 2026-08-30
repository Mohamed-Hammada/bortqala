package com.bemo.hr.assets.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;

/**
 * WP-04: one row per (asset, month) depreciation journal — the unique constraint is
 * what makes double runs of the same month post exactly one journal.
 */
@Entity
@Table(name = "fixed_asset_depreciation_posts",
        uniqueConstraints = @UniqueConstraint(name = "uq_fixed_asset_dep_post_asset_month",
                columnNames = {"app_id", "asset_id", "year_month"}))
public class FixedAssetDepreciationPost {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 36)
    private String appId;
    @Column(name = "asset_id", nullable = false, length = 36)
    private String assetId;
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    @Column(name = "journal_entry_id", nullable = false, length = 36)
    private String journalEntryId;
    @Column(name = "posted_at", nullable = false)
    private long postedAt;

    protected FixedAssetDepreciationPost() {
    }

    public FixedAssetDepreciationPost(String assetId, String yearMonth, BigDecimal amount, String journalEntryId) {
        this.id = java.util.UUID.randomUUID().toString();
        this.assetId = assetId;
        this.yearMonth = yearMonth;
        this.amount = amount;
        this.journalEntryId = journalEntryId;
        this.postedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getAssetId() { return assetId; }
    public String getYearMonth() { return yearMonth; }
    public BigDecimal getAmount() { return amount; }
    public String getJournalEntryId() { return journalEntryId; }
    public long getPostedAt() { return postedAt; }
}
