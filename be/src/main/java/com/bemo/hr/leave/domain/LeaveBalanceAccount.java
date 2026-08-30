package com.bemo.hr.leave.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "leave_balance_accounts")
public class LeaveBalanceAccount {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "employee_id", nullable = false, length = 36)
    private String employeeId;

    @Column(name = "leave_type_id", nullable = false, length = 36)
    private String leaveTypeId;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "entitled_days", nullable = false, precision = 6, scale = 2)
    private BigDecimal entitledDays;

    @Column(name = "carried_over_days", nullable = false, precision = 6, scale = 2)
    private BigDecimal carriedOverDays;

    @Column(name = "used_days", nullable = false, precision = 6, scale = 2)
    private BigDecimal usedDays;

    @Column(name = "pending_days", nullable = false, precision = 6, scale = 2)
    private BigDecimal pendingDays;

    @Column(name = "remaining_days", nullable = false, precision = 6, scale = 2)
    private BigDecimal remainingDays;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected LeaveBalanceAccount() {
    }

    public LeaveBalanceAccount(String employeeId, String leaveTypeId, int year, BigDecimal entitledDays, BigDecimal carriedOverDays) {
        this.id = UUID.randomUUID().toString();
        this.employeeId = employeeId;
        this.leaveTypeId = leaveTypeId;
        this.year = year;
        this.entitledDays = entitledDays != null ? entitledDays : new BigDecimal("21.0");
        this.carriedOverDays = carriedOverDays != null ? carriedOverDays : BigDecimal.ZERO;
        this.usedDays = BigDecimal.ZERO;
        this.pendingDays = BigDecimal.ZERO;
        this.remainingDays = this.entitledDays.add(this.carriedOverDays);
    }

    public void reserveDays(BigDecimal days) {
        this.pendingDays = this.pendingDays.add(days);
        recalcRemaining();
    }

    public void unreserveDays(BigDecimal days) {
        this.pendingDays = this.pendingDays.subtract(days).max(BigDecimal.ZERO);
        recalcRemaining();
    }

    public void consumeDays(BigDecimal days) {
        this.pendingDays = this.pendingDays.subtract(days).max(BigDecimal.ZERO);
        this.usedDays = this.usedDays.add(days);
        recalcRemaining();
    }

    public void restoreConsumedDays(BigDecimal days) {
        this.usedDays = this.usedDays.subtract(days).max(BigDecimal.ZERO);
        recalcRemaining();
    }

    private void recalcRemaining() {
        this.remainingDays = this.entitledDays.add(this.carriedOverDays).subtract(this.usedDays).subtract(this.pendingDays);
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

    public String getEmployeeId() {
        return employeeId;
    }

    public String getLeaveTypeId() {
        return leaveTypeId;
    }

    public int getYear() {
        return year;
    }

    public BigDecimal getEntitledDays() {
        return entitledDays;
    }

    public BigDecimal getCarriedOverDays() {
        return carriedOverDays;
    }

    public BigDecimal getUsedDays() {
        return usedDays;
    }

    public BigDecimal getPendingDays() {
        return pendingDays;
    }

    public BigDecimal getRemainingDays() {
        return remainingDays;
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
