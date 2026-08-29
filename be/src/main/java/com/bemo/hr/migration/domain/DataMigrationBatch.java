package com.bemo.hr.migration.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sys_data_migration_batches")
public class DataMigrationBatch {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 40, nullable = false)
    private MigrationEntityType entityType;

    @Column(name = "status", length = 30, nullable = false)
    private String status; // DRAFT, VALIDATED, DRY_RUN_PASSED, COMMITTED, ROLLED_BACK, FAILED

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "total_records", nullable = false)
    private int totalRecords;

    @Column(name = "imported_records", nullable = false)
    private int importedRecords;

    @Column(name = "rejected_records", nullable = false)
    private int rejectedRecords;

    @Column(name = "duplicate_records", nullable = false)
    private int duplicateRecords;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "gl_account_code", length = 50)
    private String glAccountCode;

    @Column(name = "gl_balance_match", nullable = false)
    private boolean glBalanceMatch;

    @Column(name = "created_by", length = 100, nullable = false)
    private String createdBy;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public DataMigrationBatch(String id, MigrationEntityType entityType, String fileName, String createdBy) {
        this.id = id;
        this.entityType = entityType;
        this.fileName = fileName;
        this.createdBy = createdBy;
        this.status = "DRAFT";
        this.totalRecords = 0;
        this.importedRecords = 0;
        this.rejectedRecords = 0;
        this.duplicateRecords = 0;
        this.totalAmount = BigDecimal.ZERO;
        this.glBalanceMatch = true;
        this.startedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
