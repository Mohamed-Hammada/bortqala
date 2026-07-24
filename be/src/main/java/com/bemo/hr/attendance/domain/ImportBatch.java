package com.bemo.hr.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_batches")
public class ImportBatch {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(nullable = false, unique = true, length = 64)
    private String checksum;
    @Column(name = "file_name", nullable = false)
    private String fileName;
    @Column(name = "device_name", nullable = false, length = 150)
    private String deviceName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportStatus status;
    @Column(name = "total_rows", nullable = false)
    private int totalRows;
    @Column(name = "imported_rows", nullable = false)
    private int importedRows;
    @Column(name = "error_rows", nullable = false)
    private int errorRows;
    @Column(name = "imported_by", nullable = false, length = 100)
    private String importedBy;
    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    protected ImportBatch() {
    }

    public ImportBatch(String checksum, String fileName, String deviceName, String importedBy,
                       int totalRows, int importedRows, int errorRows) {
        this.id = UUID.randomUUID().toString();
        this.checksum = checksum;
        this.fileName = fileName;
        this.deviceName = deviceName.strip();
        this.importedBy = importedBy.strip();
        this.totalRows = totalRows;
        this.importedRows = importedRows;
        this.errorRows = errorRows;
        this.status = errorRows == 0 ? ImportStatus.COMPLETED : ImportStatus.COMPLETED_WITH_ERRORS;
    }

    @PrePersist
    void prePersist() { importedAt = Instant.now(); }

    public String getId() { return id; }
    public String getChecksum() { return checksum; }
    public String getFileName() { return fileName; }
    public String getDeviceName() { return deviceName; }
    public ImportStatus getStatus() { return status; }
    public int getTotalRows() { return totalRows; }
    public int getImportedRows() { return importedRows; }
    public int getErrorRows() { return errorRows; }
    public String getImportedBy() { return importedBy; }
    public Instant getImportedAt() { return importedAt; }
}
