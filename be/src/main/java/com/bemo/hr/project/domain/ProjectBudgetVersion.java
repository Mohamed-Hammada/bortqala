package com.bemo.hr.project.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Entity
@Table(name = "project_budget_versions")
public class ProjectBudgetVersion {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "project_id", length = 36, nullable = false)
    private String projectId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "version_name", length = 100, nullable = false)
    private String versionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private BudgetVersionStatus status;

    @Column(name = "approved_by_user_id", length = 36)
    private String approvedByUserId;

    @Column(name = "approved_at")
    private Long approvedAt;

    @Column(name = "total_budget_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalBudgetAmount;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ProjectBudgetVersion() {
    }

    public ProjectBudgetVersion(String projectId, int versionNumber, String versionName, String notes) {
        this.id = UUID.randomUUID().toString();
        this.projectId = projectId;
        this.versionNumber = versionNumber > 0 ? versionNumber : 1;
        this.versionName = versionName != null ? versionName.strip() : ("Budget Revision V" + this.versionNumber);
        this.status = BudgetVersionStatus.DRAFT;
        this.totalBudgetAmount = BigDecimal.ZERO;
        this.notes = notes != null ? notes.strip() : null;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateTotalBudget(BigDecimal total) {
        if (this.status != BudgetVersionStatus.DRAFT) {
            throw new BusinessRuleException("CANNOT_MODIFY_NON_DRAFT_BUDGET_VERSION");
        }
        this.totalBudgetAmount = total != null ? total : BigDecimal.ZERO;
        this.updatedAt = System.currentTimeMillis();
    }

    public void approve(String userId) {
        if (this.status != BudgetVersionStatus.DRAFT) {
            throw new BusinessRuleException("BUDGET_VERSION_ALREADY_APPROVED_OR_SUPERSEDED");
        }
        this.status = BudgetVersionStatus.APPROVED;
        this.approvedByUserId = userId;
        this.approvedAt = System.currentTimeMillis();
        this.updatedAt = this.approvedAt;
    }

    public void supersede() {
        this.status = BudgetVersionStatus.SUPERSEDED;
        this.updatedAt = System.currentTimeMillis();
    }
}
