package com.bemo.hr.medical.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patient_documents")
@Getter
@Setter
@NoArgsConstructor
public class PatientDocument {

    public enum DocumentKind {
        LAB,
        IMAGING,
        REPORT,
        CONSENT
    }

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "visit_id", length = 64)
    private String visitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_kind", length = 32, nullable = false)
    private DocumentKind documentKind;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "uploaded_at", nullable = false)
    private long uploadedAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public PatientDocument(String patientId,
                           String visitId,
                           DocumentKind documentKind,
                           String fileName,
                           String contentType,
                           Long fileSize,
                           String storagePath,
                           String notes) {
        this.id = UUID.randomUUID().toString();
        this.patientId = patientId;
        this.visitId = visitId;
        this.documentKind = documentKind != null ? documentKind : DocumentKind.REPORT;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize != null ? fileSize : 0L;
        this.storagePath = storagePath;
        this.notes = notes;
        this.uploadedAt = Instant.now().toEpochMilli();
        this.createdAt = this.uploadedAt;
        this.updatedAt = this.uploadedAt;
        this.version = 0L;
    }
}
