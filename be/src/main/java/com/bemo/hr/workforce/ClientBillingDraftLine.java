package com.bemo.hr.workforce;

import com.bemo.hr.shared.security.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One worker's billable row within a generated {@link ClientBillingPeriod}.
 * Approved billing days are the attendance units recorded in manually-entered
 * attendance whose work date falls inside an APP (approved/locked) settlement
 * period window for the worker's month. A row with {@code lineStatus =
 * MISSING_RATE} has days but no effective rate and blocks the period confirmation.
 */
@Entity
@Table(name = "client_billing_draft_lines")
public class ClientBillingDraftLine {

    public static final String LINE_BILLABLE = "BILLABLE";
    public static final String LINE_MISSING_RATE = "MISSING_RATE";

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "app_id", nullable = false, length = 36)
    private String appId;

    @Column(name = "billing_period_id", nullable = false, length = 36)
    private String billingPeriodId;

    @Column(name = "worker_id", nullable = false, length = 36)
    private String workerId;

    @Column(name = "worker_code", length = 50)
    private String workerCode;

    @Column(name = "full_name", length = 160)
    private String fullName;

    @Column(name = "category_id", length = 36)
    private String categoryId;

    @Column(name = "category_name", length = 160)
    private String categoryName;

    @Column(name = "approved_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal approvedDays;

    @Column(name = "day_rate", precision = 12, scale = 2)
    private BigDecimal dayRate;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "wage_cost", precision = 15, scale = 2)
    private BigDecimal wageCost;

    @Column(name = "variance_amount", precision = 15, scale = 2)
    private BigDecimal varianceAmount;

    @Column(name = "line_status", nullable = false, length = 20)
    private String lineStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ClientBillingDraftLine() {
    }

    public ClientBillingDraftLine(String id, String billingPeriodId, String workerId, String workerCode,
                                  String fullName, String categoryId, String categoryName,
                                  BigDecimal approvedDays, BigDecimal dayRate, BigDecimal amount,
                                  BigDecimal wageCost, BigDecimal varianceAmount, String lineStatus, String reason) {
        this.id = id;
        this.appId = TenantContext.currentOrSystem();
        this.billingPeriodId = billingPeriodId;
        this.workerId = workerId;
        this.workerCode = workerCode;
        this.fullName = fullName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.approvedDays = approvedDays;
        this.dayRate = dayRate;
        this.amount = amount;
        this.wageCost = wageCost;
        this.varianceAmount = varianceAmount;
        this.lineStatus = lineStatus;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getWorkerCode() {
        return workerCode;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getApprovedDays() {
        return approvedDays;
    }

    public BigDecimal getDayRate() {
        return dayRate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getWageCost() {
        return wageCost;
    }

    public BigDecimal getVarianceAmount() {
        return varianceAmount;
    }

    public String getLineStatus() {
        return lineStatus;
    }

    public String getReason() {
        return reason;
    }
}