package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "project_schedule_tasks")
public class ProjectScheduleTask {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "schedule_id", length = 36, nullable = false)
    private String scheduleId;

    @Column(name = "wbs_node_id", length = 36)
    private String wbsNodeId;

    @Column(name = "parent_task_id", length = 36)
    private String parentTaskId;

    @Column(name = "task_code", length = 50, nullable = false)
    private String taskCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "early_start_date")
    private LocalDate earlyStartDate;

    @Column(name = "early_end_date")
    private LocalDate earlyEndDate;

    @Column(name = "late_start_date")
    private LocalDate lateStartDate;

    @Column(name = "late_end_date")
    private LocalDate lateEndDate;

    @Column(name = "free_float_days", nullable = false)
    private int freeFloatDays;

    @Column(name = "total_float_days", nullable = false)
    private int totalFloatDays;

    @Column(name = "is_critical", nullable = false)
    private boolean critical;

    @Column(name = "percent_complete", precision = 5, scale = 2, nullable = false)
    private BigDecimal percentComplete;

    @Column(name = "is_milestone", nullable = false)
    private boolean milestone;

    @Enumerated(EnumType.STRING)
    @Column(name = "constraint_type", length = 30, nullable = false)
    private TaskConstraintType constraintType;

    @Column(name = "constraint_date")
    private LocalDate constraintDate;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ProjectScheduleTask() {
    }

    public ProjectScheduleTask(String scheduleId, String wbsNodeId, String parentTaskId,
                               String taskCode, String name, String nameEn,
                               int durationDays, LocalDate plannedStartDate, LocalDate plannedEndDate,
                               boolean milestone, TaskConstraintType constraintType,
                               LocalDate constraintDate, int sortOrder) {
        this.id = UUID.randomUUID().toString();
        this.scheduleId = scheduleId;
        this.wbsNodeId = wbsNodeId;
        this.parentTaskId = parentTaskId;
        this.taskCode = taskCode != null ? taskCode.strip() : "TSK";
        this.name = name != null ? name.strip() : "Task";
        this.nameEn = nameEn != null ? nameEn.strip() : null;
        this.durationDays = Math.max(0, durationDays);
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate != null ? plannedEndDate : (plannedStartDate != null ? plannedStartDate.plusDays(Math.max(1, durationDays) - 1) : null);
        this.earlyStartDate = plannedStartDate;
        this.earlyEndDate = this.plannedEndDate;
        this.lateStartDate = plannedStartDate;
        this.lateEndDate = this.plannedEndDate;
        this.freeFloatDays = 0;
        this.totalFloatDays = 0;
        this.critical = false;
        this.percentComplete = BigDecimal.ZERO;
        this.milestone = milestone;
        this.constraintType = constraintType != null ? constraintType : TaskConstraintType.ASAP;
        this.constraintDate = constraintDate;
        this.sortOrder = sortOrder;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateDetails(String wbsNodeId, String parentTaskId, String taskCode,
                              String name, String nameEn, int durationDays,
                              LocalDate plannedStartDate, LocalDate plannedEndDate,
                              boolean milestone, TaskConstraintType constraintType,
                              LocalDate constraintDate, int sortOrder) {
        this.wbsNodeId = wbsNodeId;
        this.parentTaskId = parentTaskId;
        if (taskCode != null && !taskCode.isBlank()) this.taskCode = taskCode.strip();
        if (name != null && !name.isBlank()) this.name = name.strip();
        this.nameEn = nameEn != null && !nameEn.isBlank() ? nameEn.strip() : null;
        this.durationDays = Math.max(0, durationDays);
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate;
        this.milestone = milestone;
        if (constraintType != null) this.constraintType = constraintType;
        this.constraintDate = constraintDate;
        this.sortOrder = sortOrder;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setCpmCalculations(LocalDate earlyStart, LocalDate earlyEnd,
                                   LocalDate lateStart, LocalDate lateEnd,
                                   int freeFloat, int totalFloat, boolean critical) {
        this.earlyStartDate = earlyStart;
        this.earlyEndDate = earlyEnd;
        this.lateStartDate = lateStart;
        this.lateEndDate = lateEnd;
        this.freeFloatDays = freeFloat;
        this.totalFloatDays = totalFloat;
        this.critical = critical;
        this.updatedAt = System.currentTimeMillis();
    }

    public void updatePercentComplete(BigDecimal percentComplete) {
        if (percentComplete != null) {
            this.percentComplete = percentComplete.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
            this.updatedAt = System.currentTimeMillis();
        }
    }
}
