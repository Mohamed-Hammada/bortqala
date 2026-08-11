package com.bemo.hr.finance.domain.journal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "journal_source_metadata")
public class JournalSourceMetadata {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "journal_id", nullable = false, length = 36)
    private String journalId;

    @Column(name = "source_document_type", nullable = false, length = 50)
    private String sourceDocumentType;

    @Column(name = "source_document_id", nullable = false, length = 36)
    private String sourceDocumentId;

    @Column(name = "immutable_lock", nullable = false)
    private boolean immutableLock = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected JournalSourceMetadata() {}

    public JournalSourceMetadata(String journalId, String sourceDocumentType, String sourceDocumentId) {
        this.id = UUID.randomUUID().toString();
        this.journalId = journalId;
        this.sourceDocumentType = sourceDocumentType;
        this.sourceDocumentId = sourceDocumentId;
        this.immutableLock = true;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getJournalId() { return journalId; }
    public String getSourceDocumentType() { return sourceDocumentType; }
    public String getSourceDocumentId() { return sourceDocumentId; }
    public boolean isImmutableLock() { return immutableLock; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
