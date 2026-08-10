package com.bemo.hr.manufacturing.production.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "boms")
public class BomHeader {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "bom_code", nullable = false, length = 50)
    private String bomCode;

    @Column(name = "finished_item_id", length = 36)
    private String finishedItemId;

    @Column(name = "finished_good_name", nullable = false, length = 255)
    private String finishedGoodName;

    @Column(name = "yield_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal yieldQuantity;

    @Column(name = "revision", nullable = false, length = 20)
    private String revision;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @OneToMany(mappedBy = "bomHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BomLine> lines = new ArrayList<>();

    protected BomHeader() {}

    public BomHeader(String bomCode, String finishedItemId, String finishedGoodName,
                     BigDecimal yieldQuantity, String revision, LocalDate effectiveFrom,
                     LocalDate effectiveTo, String notes, boolean active, List<BomLine> lines) {
        this.id = UUID.randomUUID().toString();
        this.revision = revision == null || revision.isBlank() ? "v1.0" : revision.strip();
        this.lines = new ArrayList<>();
        update(bomCode, finishedItemId, finishedGoodName, yieldQuantity, revision, effectiveFrom, effectiveTo, notes, active, lines);
    }

    public void update(String bomCode, String finishedItemId, String finishedGoodName,
                       BigDecimal yieldQuantity, String revision, LocalDate effectiveFrom,
                       LocalDate effectiveTo, String notes, boolean active, List<BomLine> lines) {
        this.bomCode = bomCode.strip();
        this.finishedItemId = finishedItemId;
        this.finishedGoodName = finishedGoodName.strip();
        this.yieldQuantity = yieldQuantity == null ? BigDecimal.ONE : yieldQuantity;
        if (revision != null && !revision.isBlank()) {
            this.revision = revision.strip();
        }
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.notes = notes == null ? null : notes.strip();
        this.active = active;
        if (lines != null) {
            this.lines.clear();
            lines.forEach(this::addLine);
        }
    }

    public void addLine(BomLine line) {
        line.attachTo(this);
        this.lines.add(line);
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getBomCode() { return bomCode; }
    public String getFinishedItemId() { return finishedItemId; }
    public String getFinishedGoodName() { return finishedGoodName; }
    public BigDecimal getYieldQuantity() { return yieldQuantity; }
    public String getRevision() { return revision; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public String getNotes() { return notes; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public List<BomLine> getLines() { return lines; }
}
