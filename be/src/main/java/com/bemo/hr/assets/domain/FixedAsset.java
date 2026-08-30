package com.bemo.hr.assets.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * WP-04: depreciable fixed asset (roadmap B-4). Straight-line depreciation v1:
 * monthly charge = (cost − salvage) / usefulLifeMonths, with the exact remainder
 * posted in the final month so the life never drifts. The server caches
 * accumulated depreciation and the last posted month to keep runs idempotent.
 */
@Entity
@Table(name = "fixed_assets")
public class FixedAsset {

    public enum Category {
        VEHICLE, MACHINERY, EQUIPMENT, BUILDING, OTHER
    }

    public enum DepreciationMethod {
        STRAIGHT_LINE
    }

    public enum Status {
        ACTIVE, FULLY_DEPRECIATED, DISPOSED
    }

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 36)
    private String appId;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, length = 20)
    private String category;
    @Column(name = "acquisition_date", nullable = false)
    private long acquisitionDate;
    @Column(name = "acquisition_cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal acquisitionCost;
    @Column(name = "salvage_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal salvageValue;
    @Column(name = "useful_life_months", nullable = false)
    private int usefulLifeMonths;
    @Column(nullable = false, length = 20)
    private String method;
    @Column(name = "accumulated_depreciation", nullable = false, precision = 18, scale = 2)
    private BigDecimal accumulatedDepreciation;
    @Column(name = "last_posted_year_month", length = 7)
    private String lastPostedYearMonth;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "disposal_date")
    private Long disposalDate;
    @Column(name = "disposal_proceeds", precision = 18, scale = 2)
    private BigDecimal disposalProceeds;
    @Column(name = "branch_id", length = 36)
    private String branchId;
    @Column(name = "cost_center_id", length = 36)
    private String costCenterId;
    @Version
    private Long version;

    protected FixedAsset() {
    }

    public FixedAsset(String name, Category category, long acquisitionDate, BigDecimal acquisitionCost,
                      BigDecimal salvageValue, int usefulLifeMonths, String branchId, String costCenterId) {
        this.id = java.util.UUID.randomUUID().toString();
        update(name, category, acquisitionDate, acquisitionCost, salvageValue, usefulLifeMonths, branchId, costCenterId);
        this.status = Status.ACTIVE.name();
        this.accumulatedDepreciation = BigDecimal.ZERO.setScale(2);
    }

    public void update(String name, Category category, long acquisitionDate, BigDecimal acquisitionCost,
                       BigDecimal salvageValue, int usefulLifeMonths, String branchId, String costCenterId) {
        if (acquisitionCost == null || acquisitionCost.signum() <= 0)
            throw new BusinessRuleException("Acquisition cost must be greater than zero.",
                    "ASSET_COST_INVALID", org.springframework.http.HttpStatus.BAD_REQUEST);
        if (salvageValue == null || salvageValue.signum() < 0
                || salvageValue.compareTo(acquisitionCost) >= 0)
            throw new BusinessRuleException("Salvage value must be zero or more but less than the acquisition cost.",
                    "ASSET_SALVAGE_INVALID", org.springframework.http.HttpStatus.BAD_REQUEST);
        if (usefulLifeMonths < 1 || usefulLifeMonths > 480)
            throw new BusinessRuleException("Useful life must be between 1 and 480 months.",
                    "ASSET_LIFE_INVALID", org.springframework.http.HttpStatus.BAD_REQUEST);
        this.name = name.strip();
        this.category = category.name();
        this.acquisitionDate = acquisitionDate;
        this.acquisitionCost = acquisitionCost;
        this.salvageValue = salvageValue;
        this.usefulLifeMonths = usefulLifeMonths;
        this.method = DepreciationMethod.STRAIGHT_LINE.name();
        this.branchId = blank(branchId);
        this.costCenterId = blank(costCenterId);
    }

    /** First month that carries a charge — the month after acquisition. */
    public YearMonth firstChargeMonth() {
        LocalDate date = Instant.ofEpochMilli(acquisitionDate).atZone(ZoneOffset.UTC).toLocalDate();
        return YearMonth.from(date).plusMonths(1);
    }

    public YearMonth finalChargeMonth() {
        return firstChargeMonth().plusMonths(usefulLifeMonths - 1);
    }

    /** Straight-line monthly charge rounded half-up; the final month posts the remainder instead. */
    public java.math.BigDecimal monthlyCharge() {
        return depreciableBase()
                .divide(java.math.BigDecimal.valueOf(usefulLifeMonths), 2, java.math.RoundingMode.HALF_UP);
    }

    public java.math.BigDecimal depreciableBase() {
        return acquisitionCost.subtract(salvageValue);
    }

    /** Charge due for {@code yearMonth}, or zero when nothing remains / outside the life. */
    public java.math.BigDecimal chargeFor(YearMonth yearMonth) {
        if (status != Status.ACTIVE.name()) return BigDecimal.ZERO;
        if (yearMonth.isBefore(firstChargeMonth()) || yearMonth.isAfter(finalChargeMonth())) return BigDecimal.ZERO;
        java.math.BigDecimal remaining = depreciableBase().subtract(accumulatedDepreciation);
        if (remaining.signum() <= 0) return BigDecimal.ZERO;
        if (yearMonth.equals(finalChargeMonth()) || remaining.compareTo(monthlyCharge()) < 0) return remaining;
        return monthlyCharge();
    }

    /** Registers a posted charge; flips to FULLY_DEPRECIATED once the base is consumed. */
    public void registerPostedCharge(YearMonth yearMonth, java.math.BigDecimal amount) {
        this.accumulatedDepreciation = this.accumulatedDepreciation.add(amount);
        this.lastPostedYearMonth = yearMonth.toString();
        if (this.accumulatedDepreciation.compareTo(depreciableBase()) >= 0) {
            this.status = Status.FULLY_DEPRECIATED.name();
        }
    }

    /** Disposal from ACTIVE/FULLY_DEPRECIATED only; proceeds may be any amount ≥ 0. */
    public void dispose(long disposalDate, java.math.BigDecimal proceeds) {
        if (this.status == Status.DISPOSED.name())
            throw new BusinessRuleException("This asset has already been disposed of.",
                    "ASSET_DISPOSAL_INVALID", org.springframework.http.HttpStatus.CONFLICT);
        if (proceeds == null || proceeds.signum() < 0)
            throw new BusinessRuleException("Disposal proceeds cannot be negative.",
                    "ASSET_DISPOSAL_INVALID", org.springframework.http.HttpStatus.BAD_REQUEST);
        this.disposalDate = disposalDate;
        this.disposalProceeds = proceeds;
        this.status = Status.DISPOSED.name();
    }

    /** Gain when positive, loss when negative — computed backend-side for the disposal journal. */
    public java.math.BigDecimal disposalGainOrLoss() {
        if (disposalProceeds == null) return null;
        return disposalProceeds.subtract(netBookValue());
    }

    public java.math.BigDecimal netBookValue() {
        return acquisitionCost.subtract(accumulatedDepreciation);
    }

    public boolean isActive() {
        return Status.ACTIVE.name().equals(status);
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getName() { return name; }
    public Category getCategory() { return Category.valueOf(category); }
    public long getAcquisitionDate() { return acquisitionDate; }
    public java.math.BigDecimal getAcquisitionCost() { return acquisitionCost; }
    public java.math.BigDecimal getSalvageValue() { return salvageValue; }
    public int getUsefulLifeMonths() { return usefulLifeMonths; }
    public DepreciationMethod getMethod() { return DepreciationMethod.valueOf(method); }
    public java.math.BigDecimal getAccumulatedDepreciation() { return accumulatedDepreciation; }
    public String getLastPostedYearMonth() { return lastPostedYearMonth; }
    public Status getStatus() { return Status.valueOf(status); }
    public Long getDisposalDate() { return disposalDate; }
    public java.math.BigDecimal getDisposalProceeds() { return disposalProceeds; }
    public String getBranchId() { return branchId; }
    public String getCostCenterId() { return costCenterId; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAtUtc() { return LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneOffset.UTC); }
}
