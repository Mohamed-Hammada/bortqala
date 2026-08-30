package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Getter
@Entity
@Table(name = "project_budget_lines")
public class ProjectBudgetLine {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "budget_version_id", length = 36, nullable = false)
    private String budgetVersionId;

    @Column(name = "project_id", length = 36, nullable = false)
    private String projectId;

    @Column(name = "wbs_node_id", length = 36)
    private String wbsNodeId;

    @Column(name = "cost_code_id", length = 36)
    private String costCodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_category", length = 30, nullable = false)
    private CostCategory costCategory;

    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Column(name = "budget_quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal budgetQuantity;

    @Column(name = "unit_of_measure", length = 30, nullable = false)
    private String unitOfMeasure;

    @Column(name = "budget_unit_rate", precision = 18, scale = 4, nullable = false)
    private BigDecimal budgetUnitRate;

    @Column(name = "budget_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal budgetAmount;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ProjectBudgetLine() {
    }

    public ProjectBudgetLine(String budgetVersionId, String projectId, String wbsNodeId,
                             String costCodeId, CostCategory costCategory, String description,
                             BigDecimal budgetQuantity, String unitOfMeasure,
                             BigDecimal budgetUnitRate, int sortOrder) {
        this.id = UUID.randomUUID().toString();
        this.budgetVersionId = budgetVersionId;
        this.projectId = projectId;
        this.wbsNodeId = wbsNodeId;
        this.costCodeId = costCodeId;
        this.costCategory = costCategory != null ? costCategory : CostCategory.MATERIAL;
        this.description = description != null ? description.strip() : "";
        this.budgetQuantity = budgetQuantity != null ? budgetQuantity : BigDecimal.ZERO;
        this.unitOfMeasure = unitOfMeasure != null ? unitOfMeasure.strip() : "PCS";
        this.budgetUnitRate = budgetUnitRate != null ? budgetUnitRate : BigDecimal.ZERO;
        this.budgetAmount = this.budgetQuantity.multiply(this.budgetUnitRate).setScale(2, RoundingMode.HALF_UP);
        this.sortOrder = sortOrder;
    }
}
