package com.bemo.hr.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "daily_report_attachments")
public class DailyReportAttachment {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "daily_report_id", length = 36, nullable = false)
    private String dailyReportId;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "content_type", length = 100, nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "file_content", nullable = false)
    private byte[] fileContent;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "uploaded_by", length = 64)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected DailyReportAttachment() {
    }

    public DailyReportAttachment(String dailyReportId, String fileName, String contentType,
                                 byte[] fileContent, String description, String uploadedBy) {
        this.id = UUID.randomUUID().toString();
        this.dailyReportId = Objects.requireNonNull(dailyReportId, "dailyReportId must not be null");
        this.fileName = Objects.requireNonNull(fileName, "fileName must not be null");
        this.contentType = contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream";
        Objects.requireNonNull(fileContent, "fileContent must not be null");
        this.fileContent = fileContent.clone();
        this.fileSize = fileContent.length;
        this.description = description;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = Instant.now();
    }

    // ─── Getters ─────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDailyReportId() {
        return dailyReportId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    /** Defensive copy — never expose the backing array directly. */
    public byte[] contentCopy() {
        return fileContent.clone();
    }

    public String getDescription() {
        return description;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
