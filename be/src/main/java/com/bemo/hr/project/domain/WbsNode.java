package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "wbs_nodes")
public class WbsNode {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "parent_id", length = 36)
    private String parentId;

    @Column(name = "wbs_code", nullable = false, length = 50)
    private String wbsCode;

    @Column(name = "wbs_path", nullable = false, length = 500)
    private String wbsPath;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_en")
    private String nameEn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 30)
    private WbsNodeType nodeType = WbsNodeType.WORK_PACKAGE;

    @Column(nullable = false)
    private int level = 1;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "unit_of_measure", length = 30)
    private String unitOfMeasure;

    @Column(name = "planned_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal plannedQuantity = BigDecimal.ZERO;

    @Column(name = "unit_rate", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitRate = BigDecimal.ZERO;

    @Column(name = "planned_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal plannedAmount = BigDecimal.ZERO;

    @Column(name = "cost_code_id", length = 36)
    private String costCodeId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WbsNodeStatus status = WbsNodeStatus.PLANNED;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected WbsNode() {
    }

    public WbsNode(String projectId, String parentId, String wbsCode, String wbsPath,
                   String name, String nameEn, String description, WbsNodeType nodeType,
                   int level, int sortOrder, String unitOfMeasure, BigDecimal plannedQuantity,
                   BigDecimal unitRate, String costCodeId, LocalDate startDate,
                   LocalDate endDate, WbsNodeStatus status) {
        this.id = UUID.randomUUID().toString();
        this.projectId = projectId;
        this.parentId = parentId != null && !parentId.isBlank() ? parentId.strip() : null;
        this.wbsCode = wbsCode != null ? wbsCode.strip() : "";
        this.wbsPath = wbsPath != null ? wbsPath.strip() : ("/" + this.wbsCode);
        this.level = Math.max(1, level);
        this.sortOrder = sortOrder;
        update(name, nameEn, description, nodeType, unitOfMeasure, plannedQuantity,
                unitRate, costCodeId, startDate, endDate, status);
    }

    public void update(String name, String nameEn, String description, WbsNodeType nodeType,
                       String unitOfMeasure, BigDecimal plannedQuantity, BigDecimal unitRate,
                       String costCodeId, LocalDate startDate, LocalDate endDate, WbsNodeStatus status) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("WBS node name is required.");
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("WBS node end date must be after start date.");
        }
        this.name = name.strip();
        this.nameEn = nameEn != null && !nameEn.isBlank() ? nameEn.strip() : null;
        this.description = description != null && !description.isBlank() ? description.strip() : null;
        this.nodeType = nodeType != null ? nodeType : WbsNodeType.WORK_PACKAGE;
        this.unitOfMeasure = unitOfMeasure != null && !unitOfMeasure.isBlank() ? unitOfMeasure.strip() : null;
        this.plannedQuantity = plannedQuantity != null && plannedQuantity.signum() >= 0 ? plannedQuantity : BigDecimal.ZERO;
        this.unitRate = unitRate != null && unitRate.signum() >= 0 ? unitRate : BigDecimal.ZERO;
        this.costCodeId = costCodeId != null && !costCodeId.isBlank() ? costCodeId.strip() : null;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status != null ? status : WbsNodeStatus.PLANNED;
        calculatePlannedAmount();
    }

    public void reposition(String parentId, String wbsCode, String wbsPath, int level, int sortOrder) {
        this.parentId = parentId != null && !parentId.isBlank() ? parentId.strip() : null;
        this.wbsCode = wbsCode != null ? wbsCode.strip() : "";
        this.wbsPath = wbsPath != null ? wbsPath.strip() : ("/" + this.wbsCode);
        this.level = Math.max(1, level);
        this.sortOrder = sortOrder;
    }

    public void calculatePlannedAmount() {
        if (this.plannedQuantity != null && this.unitRate != null) {
            this.plannedAmount = this.plannedQuantity.multiply(this.unitRate).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.plannedAmount = BigDecimal.ZERO;
        }
    }

    public void startProgress() {
        if (this.status == WbsNodeStatus.PLANNED) {
            this.status = WbsNodeStatus.IN_PROGRESS;
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public void complete() {
        this.status = WbsNodeStatus.COMPLETED;
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateStatus(WbsNodeStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    @PrePersist
    void prePersist() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
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

    public String getProjectId() {
        return projectId;
    }

    public String getParentId() {
        return parentId;
    }

    public String getWbsCode() {
        return wbsCode;
    }

    public String getWbsPath() {
        return wbsPath;
    }

    public String getName() {
        return name;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getDescription() {
        return description;
    }

    public WbsNodeType getNodeType() {
        return nodeType;
    }

    public int getLevel() {
        return level;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public BigDecimal getPlannedQuantity() {
        return plannedQuantity;
    }

    public BigDecimal getUnitRate() {
        return unitRate;
    }

    public BigDecimal getPlannedAmount() {
        return plannedAmount;
    }

    public String getCostCodeId() {
        return costCodeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public WbsNodeStatus getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
