package com.bemo.hr.serviceops.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "srv_work_orders")
@Getter
@Setter
public class WorkOrder {

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    public enum Status {
        OPEN,
        IN_PROGRESS,
        WAITING_PARTS,
        DONE,
        DELIVERED,
        CANCELLED
    }

    @Id
    @Column(length = 36)
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;

    @Column(name = "ticket_no", nullable = false, length = 50)
    private String ticketNo;

    @Column(name = "customer_party_id", length = 36)
    private String customerPartyId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "assigned_employee_id", length = 36)
    private String assignedEmployeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(name = "promised_at", length = 30)
    private String promisedAt;

    @Column(name = "labor_total", precision = 14, scale = 2)
    private BigDecimal laborTotal;

    @Column(name = "parts_total", precision = 14, scale = 2)
    private BigDecimal partsTotal;

    @Column(name = "grand_total", precision = 14, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "invoice_id", length = 36)
    private String invoiceId;

    @Column(name = "override_note", length = 1000)
    private String overrideNote;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkOrderLaborLine> laborLines = new ArrayList<>();

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkOrderPartsLine> partsLines = new ArrayList<>();

    protected WorkOrder() {}

    public WorkOrder(String appId, String ticketNo, String customerPartyId, String customerName,
                     String title, String description, String assignedEmployeeId,
                     Priority priority, String promisedAt) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.ticketNo = ticketNo;
        this.customerPartyId = customerPartyId;
        this.customerName = customerName;
        this.title = title;
        this.description = description;
        this.assignedEmployeeId = assignedEmployeeId;
        this.priority = priority != null ? priority : Priority.NORMAL;
        this.status = Status.OPEN;
        this.promisedAt = promisedAt;
        this.laborTotal = BigDecimal.ZERO;
        this.partsTotal = BigDecimal.ZERO;
        this.grandTotal = BigDecimal.ZERO;
        this.version = 0L;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void addLaborLine(WorkOrderLaborLine line) {
        line.setWorkOrder(this);
        this.laborLines.add(line);
        recalculateTotals();
    }

    public void addPartsLine(WorkOrderPartsLine line) {
        line.setWorkOrder(this);
        this.partsLines.add(line);
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.laborTotal = laborLines.stream()
                .map(WorkOrderLaborLine::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.partsTotal = partsLines.stream()
                .map(WorkOrderPartsLine::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.grandTotal = this.laborTotal.add(this.partsTotal);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }
}
