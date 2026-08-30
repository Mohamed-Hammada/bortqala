package com.bemo.hr.serviceops.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "srv_work_order_labor_lines")
@Getter
@Setter
public class WorkOrderLaborLine {

    @Id
    @Column(length = 36)
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal hours;

    @Column(name = "hourly_rate", nullable = false, precision = 14, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    protected WorkOrderLaborLine() {}

    public WorkOrderLaborLine(String appId, String description, BigDecimal hours, BigDecimal hourlyRate) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.description = description;
        this.hours = hours != null ? hours : BigDecimal.ONE;
        this.hourlyRate = hourlyRate != null ? hourlyRate : BigDecimal.ZERO;
        this.totalAmount = this.hours.multiply(this.hourlyRate);
    }
}
