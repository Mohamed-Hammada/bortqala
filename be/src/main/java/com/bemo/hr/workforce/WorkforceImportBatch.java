package com.bemo.hr.workforce;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workforce_import_batches")
@Getter
public class WorkforceImportBatch {
    @Id
    private String id;
    @Version
    private long version;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    @Column(name = "content_type", length = 100)
    private String contentType;
    @Column(nullable = false, length = 64)
    private String checksum;
    @Lob
    @Column(name = "original_file", nullable = false)
    private byte[] originalFile;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "headers_text", length = 2000)
    private String headersText;
    @Column(name = "column_mapping", length = 2000)
    private String columnMapping;
    @Column(name = "total_rows", nullable = false)
    private int totalRows;
    @Column(name = "valid_rows", nullable = false)
    private int validRows;
    @Column(name = "invalid_rows", nullable = false)
    private int invalidRows;
    @Column(name = "imported_rows", nullable = false)
    private int importedRows;
    @Column(name = "operation_id", length = 80)
    private String operationId;
    @Column(name = "created_by", nullable = false, length = 160)
    private String createdBy;
    @Column(name = "imported_at")
    private Instant importedAt;
    @Column(name = "reversed_at")
    private Instant reversedAt;
    @Column(name = "reversed_by", length = 160)
    private String reversedBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkforceImportBatch() {
    }

    public WorkforceImportBatch(String fileName, String contentType, String checksum, byte[] originalFile,
                                String headersText, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.fileName = fileName;
        this.contentType = contentType;
        this.checksum = checksum;
        this.originalFile = originalFile;
        this.headersText = headersText;
        this.createdBy = createdBy;
        this.status = "UPLOADED";
    }

    public void map(String columnMapping) {
        this.columnMapping = columnMapping;
        this.status = "MAPPED";
    }

    public void validated(int totalRows, int validRows, int invalidRows) {
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.status = invalidRows == 0 ? "READY" : "VALIDATED";
    }

    public void readyWithAcceptedErrors() {
        this.status = "READY";
    }

    public void imported(String operationId, int importedRows) {
        this.operationId = operationId;
        this.importedRows = importedRows;
        this.status = "IMPORTED";
        this.importedAt = Instant.now();
    }

    public void reversed(String actor) {
        this.status = "REVERSED";
        this.reversedAt = Instant.now();
        this.reversedBy = actor;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
