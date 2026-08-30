package com.bemo.hr.fleet.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "flt_vehicle_documents")
public class VehicleDocument {


    public enum DocumentType {
        LICENSE, INSURANCE, INSPECTION_PASS, PERMIT
    }

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "vehicle_id", length = 64, nullable = false)
    private String vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 32, nullable = false)
    private DocumentType documentType;

    @Column(name = "document_number", length = 128, nullable = false)
    private String documentNumber;

    @Column(name = "issue_date", length = 32)
    private String issueDate;

    @Column(name = "expiry_date", length = 32, nullable = false)
    private String expiryDate;

    @Column(name = "issuer", length = 255)
    private String issuer;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected VehicleDocument() {
    }

    public VehicleDocument(String appId, String vehicleId, DocumentType documentType, String documentNumber,
                           String issueDate, String expiryDate, String issuer, String notes) {
        this.id = "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.appId = appId;
        this.vehicleId = vehicleId;
        this.documentType = documentType != null ? documentType : DocumentType.LICENSE;
        this.documentNumber = documentNumber;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.issuer = issuer;
        this.notes = notes;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getAppId() {
        return appId;
    }


    public String getId() {
        return id;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(String issueDate) {
        this.issueDate = issueDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
