package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Getter
@Entity
@Table(name = "project_forecast_eac")
public class ProjectForecastEac {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "project_id", length = 36, nullable = false)
    private String projectId;

    @Column(name = "wbs_node_id", length = 36)
    private String wbsNodeId;

    @Column(name = "cost_code_id", length = 36)
    private String costCodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_category", length = 30, nullable = false)
    private CostCategory costCategory;

    @Column(name = "budget_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal budgetAmount;

    @Column(name = "actual_cost_to_date", precision = 18, scale = 2, nullable = false)
    private BigDecimal actualCostToDate;

    @Column(name = "committed_cost", precision = 18, scale = 2, nullable = false)
    private BigDecimal committedCost;

    @Column(name = "estimate_to_complete", precision = 18, scale = 2, nullable = false)
    private BigDecimal estimateToComplete;

    @Column(name = "estimate_at_completion", precision = 18, scale = 2, nullable = false)
    private BigDecimal estimateAtCompletion;

    @Column(name = "variance_at_completion", precision = 18, scale = 2, nullable = false)
    private BigDecimal varianceAtCompletion;

    @Column(name = "forecast_profit_margin_percent", precision = 5, scale = 2)
    private BigDecimal forecastProfitMarginPercent;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected ProjectForecastEac() {
    }

    public ProjectForecastEac(String projectId, String wbsNodeId, String costCodeId,
                              CostCategory costCategory, BigDecimal budgetAmount,
                              BigDecimal actualCostToDate, BigDecimal committedCost,
                              BigDecimal estimateToComplete, String notes) {
        this.id = UUID.randomUUID().toString();
        this.projectId = projectId;
        this.wbsNodeId = wbsNodeId;
        this.costCodeId = costCodeId;
        this.costCategory = costCategory != null ? costCategory : CostCategory.MATERIAL;
        this.budgetAmount = budgetAmount != null ? budgetAmount : BigDecimal.ZERO;
        this.actualCostToDate = actualCostToDate != null ? actualCostToDate : BigDecimal.ZERO;
        this.committedCost = committedCost != null ? committedCost : BigDecimal.ZERO;
        this.estimateToComplete = estimateToComplete != null ? estimateToComplete : BigDecimal.ZERO;
        this.notes = notes != null ? notes.strip() : null;

        recalculate();
    }

    public void updateForecast(BigDecimal estimateToComplete, String notes) {
        if (estimateToComplete != null) this.estimateToComplete = estimateToComplete;
        if (notes != null) this.notes = notes.strip();
        recalculate();
    }

    public void updateActualsAndCommitments(BigDecimal budget, BigDecimal actual, BigDecimal committed) {
        if (budget != null) this.budgetAmount = budget;
        if (actual != null) this.actualCostToDate = actual;
        if (committed != null) this.committedCost = committed;
        recalculate();
    }

    private void recalculate() {
        this.estimateAtCompletion = this.actualCostToDate.add(this.estimateToComplete).setScale(2, RoundingMode.HALF_UP);
        this.varianceAtCompletion = this.budgetAmount.subtract(this.estimateAtCompletion).setScale(2, RoundingMode.HALF_UP);
        this.updatedAt = System.currentTimeMillis();
    }
}
