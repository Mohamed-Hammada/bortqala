package com.bemo.hr.migration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sys_data_migration_records")
public class DataMigrationRecord {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "batch_id", length = 36, nullable = false)
    private String batchId;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(name = "raw_json_data", columnDefinition = "TEXT", nullable = false)
    private String rawJsonData;

    @Column(name = "status", length = 30, nullable = false)
    private String status; // VALID, INVALID, DUPLICATE, COMMITTED, ROLLED_BACK

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public DataMigrationRecord(String id, String batchId, int rowNumber, String rawJsonData, String status, String errorMessage) {
        this.id = id;
        this.batchId = batchId;
        this.rowNumber = rowNumber;
        this.rawJsonData = rawJsonData;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = Instant.now();
    }
}
