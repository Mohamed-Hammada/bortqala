package com.bemo.hr.operations;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
@Getter
public class StockMovement {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "item_id", nullable = false)
    private String itemId;
    @Column(name = "party_id")
    private String partyId;
    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;
    @Column(name = "document_type", length = 30)
    private String documentType;
    @Column(name = "quantity_delta", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityDelta;
    @Column(name = "loss_percentage", precision = 7, scale = 4)
    private BigDecimal lossPercentage;
    @Column(name = "reference_code", length = 100)
    private String referenceCode;
    @Column(name = "purchase_order_no", length = 100)
    private String purchaseOrderNo;
    @Column(name = "receipt_no", length = 100)
    private String receiptNo;
    @Column(name = "delivery_note_no", length = 100)
    private String deliveryNoteNo;
    @Column(name = "invoice_no", length = 100)
    private String invoiceNo;
    @Column(name = "voucher_no", length = 100)
    private String voucherNo;
    @Column(name = "external_ref", length = 100)
    private String externalRef;
    @Column(length = 50)
    private String warehouse;
    @Column(name = "attachment_name", length = 255)
    private String attachmentName;
    @Column(name = "attachment_content_type", length = 100)
    private String attachmentContentType;
    @Column(name = "attachment_size")
    private Long attachmentSize;
    @Column(length = 1000)
    private String note;
    @Column(length = 1000)
    private String reason;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StockMovement() {
    }

    public StockMovement(String itemId, String partyId, String operationType, BigDecimal quantityDelta,
                         BigDecimal lossPercentage, String referenceCode, String note, Instant occurredAt, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.itemId = itemId;
        this.partyId = partyId;
        this.operationType = operationType.strip().toUpperCase();
        this.quantityDelta = quantityDelta;
        this.lossPercentage = lossPercentage;
        this.referenceCode = nullable(referenceCode);
        this.note = nullable(note);
        this.occurredAt = occurredAt;
        this.createdBy = createdBy;
    }

    public void assignDocument(String documentType, String reason) {
        this.documentType = documentType;
        this.reason = nullable(reason);
    }

    public void assignReferences(String purchaseOrderNo, String receiptNo, String deliveryNoteNo, String invoiceNo,
                                 String voucherNo, String externalRef, String warehouse, String attachmentName,
                                 String attachmentContentType, Long attachmentSize) {
        this.purchaseOrderNo = nullable(purchaseOrderNo);
        this.receiptNo = nullable(receiptNo);
        this.deliveryNoteNo = nullable(deliveryNoteNo);
        this.invoiceNo = nullable(invoiceNo);
        this.voucherNo = nullable(voucherNo);
        this.externalRef = nullable(externalRef);
        this.warehouse = nullable(warehouse);
        this.attachmentName = nullable(attachmentName);
        this.attachmentContentType = nullable(attachmentContentType);
        this.attachmentSize = attachmentSize;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
