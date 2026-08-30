package com.bemo.hr.compliance.privacy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "privacy_requests")
public class PrivacyRequest {

    public enum SubjectType { EMPLOYEE, PATIENT, PARTY }
    public enum Kind { EXPORT, ERASE, CONSENT_WITHDRAW }
    public enum Status { RECEIVED, IN_PROGRESS, COMPLETED, REJECTED }

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "subject_type", nullable = false, length = 20)
    private String subjectType;
    @Column(name = "subject_ref", nullable = false, length = 36)
    private String subjectRef;
    @Column(nullable = false, length = 20)
    private String kind;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "legal_note", columnDefinition = "text")
    private String legalNote;
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;
    @Column(name = "decided_by", length = 36)
    private String decidedBy;
    @Column(name = "decided_at")
    private Instant decidedAt;
    @Column(name = "export_data", columnDefinition = "text")
    private String exportData;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    private Long version;

    protected PrivacyRequest() {}

    public PrivacyRequest(String appId, SubjectType subjectType, String subjectRef, Kind kind) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.subjectType = subjectType.name();
        this.subjectRef = subjectRef;
        this.kind = kind.name();
        this.status = Status.RECEIVED.name();
        this.dueAt = Instant.now().plusSeconds(30L * 86400);
    }

    public void markInProgress() { this.status = Status.IN_PROGRESS.name(); }
    public void markCompleted(String decidedBy, String exportData) {
        this.status = Status.COMPLETED.name();
        this.decidedBy = decidedBy;
        this.decidedAt = Instant.now();
        this.exportData = exportData;
    }
    public void reject(String decidedBy, String legalNote) {
        this.status = Status.REJECTED.name();
        this.decidedBy = decidedBy;
        this.decidedAt = Instant.now();
        this.legalNote = legalNote;
    }

    public boolean isOverdue() {
        return Status.RECEIVED.name().equals(status) || Status.IN_PROGRESS.name().equals(status)
                ? Instant.now().isAfter(dueAt) : false;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public SubjectType getSubjectType() { return SubjectType.valueOf(subjectType); }
    public String getSubjectRef() { return subjectRef; }
    public Kind getKind() { return Kind.valueOf(kind); }
    public Status getStatus() { return Status.valueOf(status); }
    public String getLegalNote() { return legalNote; }
    public Instant getDueAt() { return dueAt; }
    public String getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public String getExportData() { return exportData; }
    public Long getVersion() { return version; }

    public void setStatus(String status) { this.status = status; }
    public void setLegalNote(String legalNote) { this.legalNote = legalNote; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }
}
