package com.bemo.hr.performance.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "appraisal_kpi_scores")
public class AppraisalKpiScore {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "appraisal_id", nullable = false, length = 36)
    private String appraisalId;

    @Column(name = "kpi_id", nullable = false, length = 36)
    private String kpiId;

    @Column(name = "self_rating", precision = 5, scale = 2)
    private BigDecimal selfRating;

    @Column(name = "manager_rating", precision = 5, scale = 2)
    private BigDecimal managerRating;

    @Column(name = "weighted_score", precision = 5, scale = 2)
    private BigDecimal weightedScore;

    @Column(name = "comments", length = 1000)
    private String comments;

    protected AppraisalKpiScore() {
    }

    public AppraisalKpiScore(String appraisalId, String kpiId, BigDecimal selfRating, BigDecimal managerRating, BigDecimal weightedScore, String comments) {
        this.id = UUID.randomUUID().toString();
        this.appraisalId = appraisalId;
        this.kpiId = kpiId;
        this.selfRating = selfRating;
        this.managerRating = managerRating;
        this.weightedScore = weightedScore;
        this.comments = comments;
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppraisalId() {
        return appraisalId;
    }

    public String getKpiId() {
        return kpiId;
    }

    public BigDecimal getSelfRating() {
        return selfRating;
    }

    public BigDecimal getManagerRating() {
        return managerRating;
    }

    public BigDecimal getWeightedScore() {
        return weightedScore;
    }

    public String getComments() {
        return comments;
    }
}
