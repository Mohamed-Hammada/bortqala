package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "task_resource_assignments")
public class TaskResourceAssignment {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "task_id", length = 36, nullable = false)
    private String taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", length = 30, nullable = false)
    private TaskResourceType resourceType;

    @Column(name = "resource_name", nullable = false)
    private String resourceName;

    @Column(name = "party_id", length = 36)
    private String partyId;

    @Column(name = "employee_id", length = 36)
    private String employeeId;

    @Column(name = "quantity_allocated", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantityAllocated;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected TaskResourceAssignment() {
    }

    public TaskResourceAssignment(String taskId, TaskResourceType resourceType,
                                  String resourceName, String partyId, String employeeId,
                                  BigDecimal quantityAllocated, LocalDate startDate,
                                  LocalDate endDate, String notes) {
        this.id = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.resourceType = resourceType != null ? resourceType : TaskResourceType.LABOR;
        this.resourceName = resourceName != null ? resourceName.strip() : "Resource";
        this.partyId = partyId;
        this.employeeId = employeeId;
        this.quantityAllocated = quantityAllocated != null ? quantityAllocated : BigDecimal.ONE;
        this.startDate = startDate;
        this.endDate = endDate;
        this.notes = notes != null ? notes.strip() : null;
        this.createdAt = System.currentTimeMillis();
    }
}
