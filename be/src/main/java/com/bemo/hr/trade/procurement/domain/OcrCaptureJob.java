package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "ocr_capture_jobs")
public class OcrCaptureJob {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "uploaded_by", nullable = false, length = 100)
    private String uploadedBy;
    @Column(name = "image_original_name", nullable = false, length = 255)
    private String imageOriginalName;
    @Column(name = "image_content_type", nullable = false, length = 100)
    private String imageContentType;
    @Column(name = "image_storage_path", nullable = false, length = 500)
    private String imageStoragePath;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "extracted_payload", columnDefinition = "TEXT")
    private String extractedPayload;
    @Column(name = "confidence_summary", length = 200)
    private String confidenceSummary;
    @Column(name = "error_code", length = 100)
    private String errorCode;
    @Column(name = "draft_grn_id", length = 36)
    private String draftGrnId;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected OcrCaptureJob() {
    }

    public OcrCaptureJob(String uploadedBy, String imageOriginalName, String imageContentType, String imageStoragePath) {
        this.id = UUID.randomUUID().toString();
        this.uploadedBy = uploadedBy;
        this.imageOriginalName = imageOriginalName;
        this.imageContentType = imageContentType;
        this.imageStoragePath = imageStoragePath;
        this.status = "UPLOADED";
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getUploadedBy() { return uploadedBy; }
    public String getImageOriginalName() { return imageOriginalName; }
    public String getImageContentType() { return imageContentType; }
    public String getImageStoragePath() { return imageStoragePath; }
    public String getStatus() { return status; }
    public String getExtractedPayload() { return extractedPayload; }
    public String getConfidenceSummary() { return confidenceSummary; }
    public String getErrorCode() { return errorCode; }
    public String getDraftGrnId() { return draftGrnId; }
    public long getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }

    public void setStatus(String status) { this.status = status; }
    public void setExtractedPayload(String extractedPayload) { this.extractedPayload = extractedPayload; }
    public void setConfidenceSummary(String confidenceSummary) { this.confidenceSummary = confidenceSummary; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public void setDraftGrnId(String draftGrnId) { this.draftGrnId = draftGrnId; }
}
