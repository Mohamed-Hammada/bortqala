package com.bemo.hr.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "project_site_custody_expenses")
public class SiteCustodyExpense {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "custody_id", length = 36, nullable = false)
    private String custodyId;

    @Column(name = "expense_date", nullable = false)
    private long expenseDate;

    @Column(name = "amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "category", length = 64, nullable = false)
    private String category;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "receipt_number", length = 64)
    private String receiptNumber;

    @Column(name = "recorded_by", length = 128)
    private String recordedBy;

    @Column(name = "status", length = 32, nullable = false)
    private String status; // SUBMITTED, APPROVED, REJECTED

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    public SiteCustodyExpense() {}

    public SiteCustodyExpense(
            String id,
            String tenantId,
            String custodyId,
            long expenseDate,
            BigDecimal amount,
            String category,
            String description,
            String receiptNumber,
            String recordedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.custodyId = custodyId;
        this.expenseDate = expenseDate;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.receiptNumber = receiptNumber;
        this.recordedBy = recordedBy;
        this.status = "SUBMITTED";
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

    public long getExpenseDate() {
        return expenseDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public String getStatus() {
        return status;
    }

    public void approve() {
        this.status = "APPROVED";
        this.updatedAt = System.currentTimeMillis();
    }

    public void reject() {
        this.status = "REJECTED";
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
