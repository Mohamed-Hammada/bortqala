package com.bemo.hr.compliance.eta.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "eta_invoice_submissions")
public class EtaInvoiceSubmission {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "invoice_id", nullable = false, length = 36)
    private String invoiceId;

    @Column(name = "internal_id", nullable = false, length = 50)
    private String internalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private EtaDocumentType documentType;

    @Column(name = "eta_uuid", length = 100)
    private String etaUuid;

    @Column(name = "submission_uuid", length = 100)
    private String submissionUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EtaSubmissionStatus status;

    @Column(name = "date_time_issued", nullable = false)
    private long dateTimeIssued;

    @Column(name = "total_sales_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalSalesAmount;

    @Column(name = "total_discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDiscountAmount;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "canonical_json_hash", length = 100)
    private String canonicalJsonHash;

    @Column(name = "raw_response_json", length = 4000)
    private String rawResponseJson;

    @Column(name = "validation_errors_json", length = 4000)
    private String validationErrorsJson;

    @Column(name = "submission_attempts", nullable = false)
    private int submissionAttempts = 0;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected EtaInvoiceSubmission() {
    }

    public EtaInvoiceSubmission(String invoiceId, String internalId, EtaDocumentType documentType, long dateTimeIssued,
                                BigDecimal totalSalesAmount, BigDecimal totalDiscountAmount, BigDecimal netAmount,
                                BigDecimal taxAmount, BigDecimal totalAmount, String canonicalJsonHash) {
        this.id = UUID.randomUUID().toString();
        this.invoiceId = invoiceId;
        this.internalId = internalId;
        this.documentType = documentType;
        this.dateTimeIssued = dateTimeIssued;
        this.totalSalesAmount = totalSalesAmount;
        this.totalDiscountAmount = totalDiscountAmount != null ? totalDiscountAmount : BigDecimal.ZERO;
        this.netAmount = netAmount;
        this.taxAmount = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        this.totalAmount = totalAmount;
        this.canonicalJsonHash = canonicalJsonHash;
        this.status = EtaSubmissionStatus.VALIDATED;
        this.submissionAttempts = 0;
    }

    public void markSubmitted(String submissionUuid, String etaUuid) {
        this.submissionUuid = submissionUuid;
        this.etaUuid = etaUuid;
        this.submissionAttempts++;
        this.status = EtaSubmissionStatus.SUBMITTED;
    }

    public void markValid(String rawResponseJson) {
        this.status = EtaSubmissionStatus.VALID;
        this.rawResponseJson = rawResponseJson;
        this.validationErrorsJson = null;
    }

    public void markInvalid(String validationErrorsJson, String rawResponseJson) {
        this.status = EtaSubmissionStatus.INVALID;
        this.validationErrorsJson = validationErrorsJson;
        this.rawResponseJson = rawResponseJson;
    }

    public void cancel(String cancellationReason) {
        this.status = EtaSubmissionStatus.CANCELLED;
        this.cancellationReason = cancellationReason;
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public String getInternalId() {
        return internalId;
    }

    public EtaDocumentType getDocumentType() {
        return documentType;
    }

    public String getEtaUuid() {
        return etaUuid;
    }

    public String getSubmissionUuid() {
        return submissionUuid;
    }

    public EtaSubmissionStatus getStatus() {
        return status;
    }

    public long getDateTimeIssued() {
        return dateTimeIssued;
    }

    public BigDecimal getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public BigDecimal getTotalDiscountAmount() {
        return totalDiscountAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCanonicalJsonHash() {
        return canonicalJsonHash;
    }

    public String getRawResponseJson() {
        return rawResponseJson;
    }

    public String getValidationErrorsJson() {
        return validationErrorsJson;
    }

    public int getSubmissionAttempts() {
        return submissionAttempts;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public long getVersion() {
        return version;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
