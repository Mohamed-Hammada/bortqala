package com.bemo.hr.expenses.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "expense_claims")
@Getter
@Setter
public class ExpenseClaim {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    @Column(nullable = false, length = 30)
    private String category;
    @Column(name = "spent_on", nullable = false)
    private LocalDate spentOn;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(length = 1000)
    private String description;
    @Column(name = "receipt_name", length = 255)
    private String receiptName;
    @Column(name = "receipt_content_type", length = 100)
    private String receiptContentType;
    @Column(name = "receipt_size")
    private Long receiptSize;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "approver_id", length = 100)
    private String approverId;
    @Column(name = "decided_at")
    private Instant decidedAt;
    @Column(name = "decision_note", length = 500)
    private String decisionNote;
    @Column(name = "reimbursement_reference", length = 100)
    private String reimbursementReference;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum Status {
        DRAFT, SUBMITTED, APPROVED, REJECTED, REIMBURSED;
    }

    public enum Category {
        MEAL, TRANSPORT, LODGING, SUPPLIES, OTHER;
    }

    protected ExpenseClaim() {
    }

    public ExpenseClaim(String employeeId, Category category, LocalDate spentOn, BigDecimal amount,
                        String currency, String description) {
        this.id = UUID.randomUUID().toString();
        this.employeeId = employeeId;
        this.category = category.name();
        this.spentOn = spentOn;
        this.amount = amount;
        this.currency = currency == null || currency.isBlank() ? "EGP" : currency.strip().toUpperCase();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.status = Status.DRAFT.name();
    }

    public void submit() {
        requireStatus(Status.DRAFT, "submit");
        this.status = Status.SUBMITTED.name();
    }

    public void approve(String approverId) {
        requireStatus(Status.SUBMITTED, "approve");
        this.approverId = approverId;
        this.decidedAt = Instant.now();
        this.status = Status.APPROVED.name();
    }

    public void reject(String approverId, String note) {
        requireStatus(Status.SUBMITTED, "reject");
        this.approverId = approverId;
        this.decidedAt = Instant.now();
        this.decisionNote = note == null || note.isBlank() ? null : note.strip();
        this.status = Status.REJECTED.name();
    }

    public void reimburse(String reference) {
        requireStatus(Status.APPROVED, "reimburse");
        this.reimbursementReference = reference == null || reference.isBlank() ? null : reference.strip();
        this.status = Status.REIMBURSED.name();
    }

    public void assignReceipt(String name, String contentType, Long size) {
        this.receiptName = name;
        this.receiptContentType = contentType;
        this.receiptSize = size;
    }

    public void clearReceipt() {
        this.receiptName = null;
        this.receiptContentType = null;
        this.receiptSize = null;
    }

    private void requireStatus(Status expected, String action) {
        if (Status.valueOf(this.status) != expected) {
            throw new com.bemo.hr.shared.domain.BusinessRuleException(
                    "Cannot " + action + " expense claim in " + this.status + " status.",
                    "EXPENSE_INVALID_STATE", org.springframework.http.HttpStatus.CONFLICT);
        }
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
