package com.bemo.hr.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "import_row_errors")
public class ImportRowError {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "batch_id", nullable = false)
    private String batchId;
    @Column(name = "row_number", nullable = false)
    private int rowNumber;
    @Column(nullable = false, length = 500)
    private String message;
    @Column(name = "raw_line", columnDefinition = "TEXT")
    private String rawLine;

    protected ImportRowError() {
    }

    public ImportRowError(String batchId, int rowNumber, String message, String rawLine) {
        this.id = UUID.randomUUID().toString();
        this.batchId = batchId;
        this.rowNumber = rowNumber;
        this.message = message;
        this.rawLine = rawLine;
    }

    public int getRowNumber() { return rowNumber; }
    public String getMessage() { return message; }
    public String getRawLine() { return rawLine; }
}
