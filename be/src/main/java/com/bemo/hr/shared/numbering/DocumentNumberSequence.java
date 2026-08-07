package com.bemo.hr.shared.numbering;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "document_number_sequences",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_document_number_sequence_app_type_year",
        columnNames = {"app_id", "document_type", "year"}
    )
)
public class DocumentNumberSequence {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "document_type", nullable = false, length = 40)
    private String documentType;

    @Column(nullable = false)
    private int year;

    @Column(name = "next_value", nullable = false)
    private long nextValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DocumentNumberSequence() {
    }

    public DocumentNumberSequence(String documentType, int year, long firstValue) {
        this.id = UUID.randomUUID().toString();
        this.documentType = documentType;
        this.year = year;
        this.nextValue = Math.max(1, firstValue);
    }

    public long takeNext() {
        return nextValue++;
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

    public String getDocumentType() {
        return documentType;
    }

    public int getYear() {
        return year;
    }

    public long getNextValue() {
        return nextValue;
    }
}
