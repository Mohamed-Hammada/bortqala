package com.bemo.hr.recruitment.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "recruitment_cv_files")
public class RecruitmentCvFile {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content", nullable = false)
    private byte[] content;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected RecruitmentCvFile() {
    }

    public RecruitmentCvFile(String originalName, String contentType, long sizeBytes, byte[] content) {
        this.id = UUID.randomUUID().toString();
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.content = content;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public byte[] getContent() { return content; }
    public long getCreatedAt() { return createdAt; }
}