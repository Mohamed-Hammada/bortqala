package com.bemo.hr.reporting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "holiday_proposals")
public class HolidayProposal {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "report_id", nullable = false) private String reportId;
    @Column(name = "category_id", nullable = false) private String categoryId;
    @Column(name = "category_name", nullable = false) private String categoryName;
    @Column(name = "work_date", nullable = false) private LocalDate workDate;
    @Column(name = "active_employee_count", nullable = false) private int activeEmployeeCount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private HolidayProposalStatus status;
    @Column(length = 500) private String note;
    @Column(name = "decided_by", length = 100) private String decidedBy;
    @Column(name = "decided_at") private Instant decidedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected HolidayProposal() { }
    public HolidayProposal(String reportId, String categoryId, String categoryName, LocalDate workDate, int activeEmployeeCount) {
        this.id = UUID.randomUUID().toString(); this.reportId = reportId; this.categoryId = categoryId;
        this.categoryName = categoryName; this.workDate = workDate; this.activeEmployeeCount = activeEmployeeCount;
        this.status = HolidayProposalStatus.PENDING;
    }
    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
    public void decide(HolidayProposalStatus status, String note, String actor) {
        this.status = status; this.note = note; this.decidedBy = actor; this.decidedAt = Instant.now();
    }
    public String getId() { return id; } public String getReportId() { return reportId; }
    public String getCategoryId() { return categoryId; } public String getCategoryName() { return categoryName; }
    public LocalDate getWorkDate() { return workDate; } public int getActiveEmployeeCount() { return activeEmployeeCount; }
    public HolidayProposalStatus getStatus() { return status; } public String getNote() { return note; }
    public String getDecidedBy() { return decidedBy; } public Instant getDecidedAt() { return decidedAt; }
}
