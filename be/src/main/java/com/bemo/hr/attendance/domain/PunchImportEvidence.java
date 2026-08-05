package com.bemo.hr.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable evidence that a batch supplied a punch row. A punch stays
 * deduplicated to one row while every batch that imported it keeps a claim,
 * so reversing one batch never deletes a punch another completed batch still
 * reported.
 */
@Entity
@Table(name = "punch_import_evidence")
@IdClass(PunchImportEvidence.Key.class)
public class PunchImportEvidence {
    @Id
    @Column(name = "punch_id", nullable = false)
    private String punchId;
    @Id
    @Column(name = "batch_id", nullable = false)
    private String batchId;
    @Id
    @Column(name = "row_number", nullable = false)
    private int rowNumber;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "raw_line", nullable = false, columnDefinition = "TEXT")
    private String rawLine;

    protected PunchImportEvidence() {
    }

    public PunchImportEvidence(String punchId, String batchId, String appId, int rowNumber, String rawLine) {
        this.punchId = punchId;
        this.batchId = batchId;
        this.appId = appId;
        this.rowNumber = rowNumber;
        this.rawLine = rawLine;
    }

    public String getPunchId() { return punchId; }
    public String getBatchId() { return batchId; }
    public String getAppId() { return appId; }
    public int getRowNumber() { return rowNumber; }
    public String getRawLine() { return rawLine; }

    public static class Key implements Serializable {
        private String punchId;
        private String batchId;
        private int rowNumber;

        protected Key() {
        }

        public Key(String punchId, String batchId, int rowNumber) {
            this.punchId = punchId;
            this.batchId = batchId;
            this.rowNumber = rowNumber;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key
                    && Objects.equals(punchId, key.punchId)
                    && Objects.equals(batchId, key.batchId)
                    && rowNumber == key.rowNumber;
        }

        @Override
        public int hashCode() {
            return Objects.hash(punchId, batchId, rowNumber);
        }
    }
}
