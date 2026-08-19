package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "project_cost_ledger_entries")
public class ProjectCostLedgerEntry {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", length = 30, nullable = false)
    private CostLedgerEntryType entryType;

    @Column(name = "source_module", length = 50, nullable = false)
    private String sourceModule;

    @Column(name = "source_document_id", length = 36)
    private String sourceDocumentId;

    @Column(name = "source_document_number", length = 64)
    private String sourceDocumentNumber;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Column(name = "quantity", precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_rate", precision = 18, scale = 4)
    private BigDecimal unitRate;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 10, nullable = false)
    private String currencyCode;

    @Column(name = "posted_at")
    private Long postedAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected ProjectCostLedgerEntry() {
    }

    public ProjectCostLedgerEntry(String projectId, String wbsNodeId, String costCodeId,
                                  CostCategory costCategory, CostLedgerEntryType entryType,
                                  String sourceModule, String sourceDocumentId,
                                  String sourceDocumentNumber, LocalDate entryDate,
                                  String description, BigDecimal quantity, BigDecimal unitRate,
                                  BigDecimal amount, String currencyCode) {
        this.id = UUID.randomUUID().toString();
        this.projectId = projectId;
        this.wbsNodeId = wbsNodeId;
        this.costCodeId = costCodeId;
        this.costCategory = costCategory != null ? costCategory : CostCategory.MATERIAL;
        this.entryType = entryType != null ? entryType : CostLedgerEntryType.ACTUAL;
        this.sourceModule = sourceModule != null ? sourceModule : "MANUAL";
        this.sourceDocumentId = sourceDocumentId;
        this.sourceDocumentNumber = sourceDocumentNumber;
        this.entryDate = entryDate != null ? entryDate : LocalDate.now();
        this.description = description != null ? description.strip() : "";
        this.quantity = quantity != null ? quantity : BigDecimal.ZERO;
        this.unitRate = unitRate != null ? unitRate : BigDecimal.ZERO;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.currencyCode = (currencyCode != null && !currencyCode.isBlank()) ? currencyCode.strip() : "EGP";
        long now = System.currentTimeMillis();
        this.postedAt = now;
        this.createdAt = now;
    }
}
