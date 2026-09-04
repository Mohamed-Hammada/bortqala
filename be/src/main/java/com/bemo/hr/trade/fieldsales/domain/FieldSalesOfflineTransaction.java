package com.bemo.hr.trade.fieldsales.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "field_sales_offline_transactions")
@Getter
@Setter
@NoArgsConstructor
public class FieldSalesOfflineTransaction {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "client_offline_id", length = 64, nullable = false)
    private String clientOfflineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 32, nullable = false)
    private FieldSalesDocumentType documentType;

    @Column(name = "offline_document_number", length = 64, nullable = false)
    private String offlineDocumentNumber;

    @Column(name = "server_document_id", length = 64)
    private String serverDocumentId;

    @Column(name = "server_document_number", length = 64)
    private String serverDocumentNumber;

    @Column(name = "customer_id", length = 64, nullable = false)
    private String customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "sales_rep_user_id", length = 64, nullable = false)
    private String salesRepUserId;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private FieldSalesSyncStatus status;

    @Column(name = "conflict_reason")
    private String conflictReason;

    @Lob
    @Column(name = "customer_signature_png")
    private String customerSignaturePng;

    @Column(name = "customer_confirmation_name")
    private String customerConfirmationName;

    @Column(name = "gps_coordinates", length = 64)
    private String gpsCoordinates;

    @Lob
    @Column(name = "payload_json")
    private String payloadJson;

    @Column(name = "client_created_at")
    private Instant clientCreatedAt;

    @Column(name = "synced_at")
    private Instant syncedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FieldSalesOfflineTransaction(
            String clientOfflineId,
            FieldSalesDocumentType documentType,
            String offlineDocumentNumber,
            String customerId,
            String customerName,
            String salesRepUserId,
            BigDecimal totalAmount,
            FieldSalesSyncStatus status,
            String conflictReason,
            String customerSignaturePng,
            String customerConfirmationName,
            String gpsCoordinates,
            String payloadJson,
            Instant clientCreatedAt) {
        this.id = UUID.randomUUID().toString();
        this.clientOfflineId = clientOfflineId;
        this.documentType = documentType;
        this.offlineDocumentNumber = offlineDocumentNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.salesRepUserId = salesRepUserId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.conflictReason = conflictReason;
        this.customerSignaturePng = customerSignaturePng;
        this.customerConfirmationName = customerConfirmationName;
        this.gpsCoordinates = gpsCoordinates;
        this.payloadJson = payloadJson;
        this.clientCreatedAt = clientCreatedAt;
        this.syncedAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.syncedAt == null) {
            this.syncedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void markSynced(String serverDocumentId, String serverDocumentNumber) {
        this.serverDocumentId = serverDocumentId;
        this.serverDocumentNumber = serverDocumentNumber;
        this.status = FieldSalesSyncStatus.SYNCED;
        this.conflictReason = null;
        this.syncedAt = Instant.now();
    }

    public void markConflict(String reason) {
        this.status = FieldSalesSyncStatus.CONFLICT;
        this.conflictReason = reason;
        this.syncedAt = Instant.now();
    }
}
