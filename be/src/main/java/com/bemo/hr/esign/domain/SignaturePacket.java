package com.bemo.hr.esign.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "signature_packets")
public class SignaturePacket {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "document_name", length = 300)
    private String documentName;

    @Column(name = "content_hash", length = 100)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PacketStatus status;

    @Column(name = "manifest_json", length = 8000)
    private String manifestJson;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected SignaturePacket() {
    }

    public SignaturePacket(String title, String documentName, String contentHash) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.documentName = documentName;
        this.contentHash = contentHash;
        this.status = PacketStatus.DRAFT;
    }

    public void startRouting() {
        this.status = PacketStatus.ROUTING;
    }

    public void complete(String manifestJson) {
        this.status = PacketStatus.COMPLETED;
        this.manifestJson = manifestJson;
    }

    public void reject() {
        this.status = PacketStatus.REJECTED;
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

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getTitle() { return title; }
    public String getDocumentName() { return documentName; }
    public String getContentHash() { return contentHash; }
    public PacketStatus getStatus() { return status; }
    public String getManifestJson() { return manifestJson; }
    public long getVersion() { return version; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
