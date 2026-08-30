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
 * A client billing cycle for one calendar month ({@code YYYY-MM}). The period is
 * generated as OPEN (draft lines immutable critique), then confirmed into a single
 * sales invoice and flipped to INVOICED. Statuses: OPEN | INVOICED.
 */
@Entity
@Table(name = "client_billing_periods")
public class ClientBillingPeriod {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_INVOICED = "INVOICED";

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "app_id", nullable = false, length = 36)
    private String appId;

    @Column(name = "client_party_id", nullable = false, length = 36)
    private String clientPartyId;

    @Column(nullable = false, length = 10)
    private String period;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "invoice_id", length = 36)
    private String invoiceId;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_by", nullable = false, length = 60)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ClientBillingPeriod() {
    }

    public ClientBillingPeriod(String id, String clientPartyId, String period, String createdBy) {
        this.id = id;
        this.appId = TenantContext.currentOrSystem();
        this.clientPartyId = clientPartyId;
        this.period = period;
        this.status = STATUS_OPEN;
        this.createdBy = createdBy;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markInvoiced(String invoiceId, String invoiceNumber, BigDecimal totalAmount) {
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.totalAmount = totalAmount;
        this.status = STATUS_INVOICED;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getClientPartyId() {
        return clientPartyId;
    }

    public String getPeriod() {
        return period;
    }

    public String getStatus() {
        return status;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public long getVersion() {
        return version;
    }
}