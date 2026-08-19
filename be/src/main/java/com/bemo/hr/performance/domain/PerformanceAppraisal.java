package com.bemo.hr.performance.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "performance_appraisals")
public class PerformanceAppraisal {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "cycle_id", nullable = false, length = 36)
    private String cycleId;

    @Column(name = "employee_id", nullable = false, length = 36)
    private String employeeId;

    @Column(name = "reviewer_id", length = 36)
    private String reviewerId;

    @Column(name = "self_score", precision = 5, scale = 2)
    private BigDecimal selfScore;

    @Column(name = "manager_score", precision = 5, scale = 2)
    private BigDecimal managerScore;

    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "rating_band", length = 50)
    private RatingBand ratingBand;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AppraisalStatus status;

    @Column(name = "manager_feedback", length = 2000)
    private String managerFeedback;

    @Column(name = "development_plan", length = 2000)
    private String developmentPlan;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected PerformanceAppraisal() {
    }

    public PerformanceAppraisal(String cycleId, String employeeId, String reviewerId) {
        this.id = UUID.randomUUID().toString();
        this.cycleId = cycleId;
        this.employeeId = employeeId;
        this.reviewerId = reviewerId;
        this.status = AppraisalStatus.DRAFT;
    }

    public void evaluate(BigDecimal finalScore, String managerFeedback, String developmentPlan) {
        this.finalScore = finalScore;
        this.ratingBand = RatingBand.fromScore(finalScore);
        this.managerFeedback = managerFeedback;
        this.developmentPlan = developmentPlan;
        this.status = AppraisalStatus.SUBMITTED;
    }

    public void finalizeAppraisal() {
        this.status = AppraisalStatus.FINALIZED;
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getCycleId() {
        return cycleId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public BigDecimal getSelfScore() {
        return selfScore;
    }

    public BigDecimal getManagerScore() {
        return managerScore;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public RatingBand getRatingBand() {
        return ratingBand;
    }

    public AppraisalStatus getStatus() {
        return status;
    }

    public String getManagerFeedback() {
        return managerFeedback;
    }

    public String getDevelopmentPlan() {
        return developmentPlan;
    }

    public long getVersion() {
        return version;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
