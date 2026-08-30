package com.bemo.hr.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "schedule_baseline_tasks")
public class ScheduleBaselineTask {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "baseline_id", length = 36, nullable = false)
    private String baselineId;

    @Column(name = "task_id", length = 36, nullable = false)
    private String taskId;

    @Column(name = "baseline_start_date", nullable = false)
    private LocalDate baselineStartDate;

    @Column(name = "baseline_end_date", nullable = false)
    private LocalDate baselineEndDate;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "planned_cost", precision = 15, scale = 2)
    private BigDecimal plannedCost;

    protected ScheduleBaselineTask() {
    }

    public ScheduleBaselineTask(String baselineId, String taskId,
                                LocalDate baselineStartDate, LocalDate baselineEndDate,
                                int durationDays, BigDecimal plannedCost) {
        this.id = UUID.randomUUID().toString();
        this.baselineId = baselineId;
        this.taskId = taskId;
        this.baselineStartDate = baselineStartDate;
        this.baselineEndDate = baselineEndDate;
        this.durationDays = durationDays;
        this.plannedCost = plannedCost != null ? plannedCost : BigDecimal.ZERO;
    }
}
