package com.bemo.hr.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "project_site_custody_returns")
public class SiteCustodyReturn {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "custody_id", length = 36, nullable = false)
    private String custodyId;

    @Column(name = "return_date", nullable = false)
    private long returnDate;

    @Column(name = "amount_returned", precision = 14, scale = 2, nullable = false)
    private BigDecimal amountReturned;

    @Column(name = "received_by", length = 128)
    private String receivedBy;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    public SiteCustodyReturn() {}

    public SiteCustodyReturn(
            String id,
            String tenantId,
            String custodyId,
            long returnDate,
            BigDecimal amountReturned,
            String receivedBy,
            String notes) {
        this.id = id;
        this.tenantId = tenantId;
        this.custodyId = custodyId;
        this.returnDate = returnDate;
        this.amountReturned = amountReturned;
        this.receivedBy = receivedBy;
        this.notes = notes;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getCustodyId() {
        return custodyId;
    }

    public long getReturnDate() {
        return returnDate;
    }

    public BigDecimal getAmountReturned() {
        return amountReturned;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public String getNotes() {
        return notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
