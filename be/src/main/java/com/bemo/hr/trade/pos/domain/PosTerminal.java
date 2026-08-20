package com.bemo.hr.trade.pos.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "pos_terminals")
public class PosTerminal {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "terminal_code", nullable = false, length = 50)
    private String terminalCode;

    @Column(name = "terminal_name", nullable = false, length = 150)
    private String terminalName;

    @Column(name = "branch_id", length = 36)
    private String branchId;

    @Column(name = "warehouse_id", length = 36)
    private String warehouseId;

    @Column(name = "cashbox_id", length = 36)
    private String cashboxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PosTerminalStatus status;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected PosTerminal() {
    }

    public PosTerminal(String terminalCode, String terminalName, String branchId, String warehouseId, String cashboxId) {
        this.id = UUID.randomUUID().toString();
        this.terminalCode = terminalCode;
        this.terminalName = terminalName;
        this.branchId = branchId;
        this.warehouseId = warehouseId;
        this.cashboxId = cashboxId;
        this.status = PosTerminalStatus.ACTIVE;
    }

    public void update(String terminalName, String branchId, String warehouseId, String cashboxId, PosTerminalStatus status) {
        this.terminalName = terminalName;
        this.branchId = branchId;
        this.warehouseId = warehouseId;
        this.cashboxId = cashboxId;
        this.status = status;
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

    public String getTerminalCode() {
        return terminalCode;
    }

    public String getTerminalName() {
        return terminalName;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getCashboxId() {
        return cashboxId;
    }

    public PosTerminalStatus getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
