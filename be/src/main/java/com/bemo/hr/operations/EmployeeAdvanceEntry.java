package com.bemo.hr.operations;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employee_advance_entries")
@Getter
public class EmployeeAdvanceEntry {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    @Column(name = "amount_delta", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountDelta;
    @Column(name = "entry_type", nullable = false, length = 30)
    private String entryType;
    @Column(length = 1000)
    private String note;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmployeeAdvanceEntry() {
    }

    public EmployeeAdvanceEntry(String employeeId, BigDecimal amountDelta, String entryType, String note,
                                Instant occurredAt, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.employeeId = employeeId;
        this.amountDelta = amountDelta;
        this.entryType = entryType.strip().toUpperCase();
        this.note = note == null || note.isBlank() ? null : note.strip();
        this.occurredAt = occurredAt;
        this.createdBy = createdBy;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
