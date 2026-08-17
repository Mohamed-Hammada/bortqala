package com.bemo.hr.workforce;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workforce_import_changes")
@Getter
public class WorkforceImportChange {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "batch_id", nullable = false, length = 36)
    private String batchId;
    @Column(name = "attendance_entry_id", nullable = false, length = 36)
    private String attendanceEntryId;
    @Column(name = "created_new", nullable = false)
    private boolean createdNew;
    @Column(name = "before_value", precision = 4, scale = 2)
    private BigDecimal beforeValue;
    @Column(name = "before_source", length = 30)
    private String beforeSource;
    @Column(name = "before_notes", length = 500)
    private String beforeNotes;
    @Column(name = "after_value", nullable = false, precision = 4, scale = 2)
    private BigDecimal afterValue;
    @Column(name = "reversed_at")
    private Instant reversedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkforceImportChange() {
    }

    public WorkforceImportChange(String batchId, String attendanceEntryId, boolean createdNew,
                                 BigDecimal beforeValue, String beforeSource, String beforeNotes, BigDecimal afterValue) {
        this.id = UUID.randomUUID().toString();
        this.batchId = batchId;
        this.attendanceEntryId = attendanceEntryId;
        this.createdNew = createdNew;
        this.beforeValue = beforeValue;
        this.beforeSource = beforeSource;
        this.beforeNotes = beforeNotes;
        this.afterValue = afterValue;
    }

    public void reversed() {
        this.reversedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
