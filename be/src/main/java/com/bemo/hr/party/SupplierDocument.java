package com.bemo.hr.party;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "supplier_documents")
@Getter
public class SupplierDocument {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "supplier_id", nullable = false, length = 36) private String supplierId;
    @Column(name = "document_type", nullable = false, length = 50) private String documentType;
    @Column(name = "document_number", length = 100) private String documentNumber;
    @Column(name = "file_name", nullable = false, length = 255) private String fileName;
    @Column(name = "content_type", nullable = false, length = 100) private String contentType;
    @Column(name = "file_size", nullable = false) private long fileSize;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARBINARY)
    @Column(name = "file_content", nullable = false) private byte[] fileContent;
    @Column(name = "issue_date") private LocalDate issueDate;
    @Column(name = "expiry_date") private LocalDate expiryDate;
    @Column(nullable = false) private boolean mandatory;
    @Column(nullable = false) private boolean verified;
    @Column(name = "verified_by", length = 100) private String verifiedBy;
    @Column(name = "verified_at") private Instant verifiedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected SupplierDocument() { }

    public SupplierDocument(String supplierId, String documentType, String documentNumber, String fileName,
                            String contentType, byte[] fileContent, LocalDate issueDate, LocalDate expiryDate,
                            boolean mandatory) {
        this.id = UUID.randomUUID().toString();
        this.supplierId = supplierId;
        this.documentType = documentType.strip().toUpperCase();
        this.documentNumber = nullable(documentNumber);
        this.fileName = fileName.strip();
        this.contentType = contentType;
        this.fileContent = fileContent.clone();
        this.fileSize = fileContent.length;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.mandatory = mandatory;
    }

    public void verify(String actor) {
        this.verified = true;
        this.verifiedBy = actor;
        this.verifiedAt = Instant.now();
    }

    public boolean isExpired(LocalDate today) { return expiryDate != null && expiryDate.isBefore(today); }
    public byte[] contentCopy() { return fileContent.clone(); }
    private String nullable(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    @PrePersist void prePersist() { createdAt = Instant.now(); }
}
