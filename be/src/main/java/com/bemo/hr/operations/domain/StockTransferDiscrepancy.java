package com.bemo.hr.operations.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_transfer_discrepancies")
public class StockTransferDiscrepancy {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "transfer_id", nullable = false, length = 36)
    private String transferId;

    @Column(name = "line_id", nullable = false, length = 36)
    private String lineId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(name = "expected_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal expectedQuantity;

    @Column(name = "received_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal receivedQuantity;

    @Column(name = "damaged_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal damagedQuantity;

    @Column(name = "lost_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal lostQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "discrepancy_type", nullable = false, length = 50)
    private DiscrepancyType discrepancyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_status", nullable = false, length = 30)
    private ResolutionStatus resolutionStatus = ResolutionStatus.PENDING;

    @Column(name = "reported_by", nullable = false, length = 100)
    private String reportedBy;

    @Column(name = "reported_at", nullable = false)
    private long reportedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Long resolvedAt;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    @Column(name = "journal_entry_id", length = 36)
    private String journalEntryId;

    protected StockTransferDiscrepancy() {
    }

    public StockTransferDiscrepancy(
            String transferId,
            String lineId,
            String itemId,
            BigDecimal expectedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal damagedQuantity,
            BigDecimal lostQuantity,
            DiscrepancyType discrepancyType,
            String reportedBy
    ) {
        this.id = UUID.randomUUID().toString();
        this.transferId = transferId;
        this.lineId = lineId;
        this.itemId = itemId;
        this.expectedQuantity = expectedQuantity != null ? expectedQuantity : BigDecimal.ZERO;
        this.receivedQuantity = receivedQuantity != null ? receivedQuantity : BigDecimal.ZERO;
        this.damagedQuantity = damagedQuantity != null ? damagedQuantity : BigDecimal.ZERO;
        this.lostQuantity = lostQuantity != null ? lostQuantity : BigDecimal.ZERO;
        this.discrepancyType = discrepancyType != null ? discrepancyType : DiscrepancyType.DAMAGED;
        this.resolutionStatus = ResolutionStatus.PENDING;
        this.reportedBy = reportedBy != null ? reportedBy : "SYSTEM";
        this.reportedAt = System.currentTimeMillis();
    }

    public void resolve(String resolvedBy, ResolutionStatus status, String notes, String journalEntryId) {
        this.resolvedBy = resolvedBy != null ? resolvedBy : "SYSTEM";
        this.resolutionStatus = status != null ? status : ResolutionStatus.RESOLVED;
        this.resolutionNotes = notes == null ? null : notes.strip();
        this.journalEntryId = journalEntryId;
        this.resolvedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getLineId() {
        return lineId;
    }

    public String getItemId() {
        return itemId;
    }

    public BigDecimal getExpectedQuantity() {
        return expectedQuantity;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public BigDecimal getDamagedQuantity() {
        return damagedQuantity;
    }

    public BigDecimal getLostQuantity() {
        return lostQuantity;
    }

    public DiscrepancyType getDiscrepancyType() {
        return discrepancyType;
    }

    public ResolutionStatus getResolutionStatus() {
        return resolutionStatus;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public long getReportedAt() {
        return reportedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public Long getResolvedAt() {
        return resolvedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public String getJournalEntryId() {
        return journalEntryId;
    }

    public enum DiscrepancyType {
        DAMAGED,
        LOST,
        SHORTAGE,
        SURPLUS
    }

    public enum ResolutionStatus {
        PENDING,
        RESOLVED,
        WRITTEN_OFF,
        CLAIMED
    }
}
