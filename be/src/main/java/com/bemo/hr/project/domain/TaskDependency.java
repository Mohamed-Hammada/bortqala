package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Getter
@Entity
@Table(name = "task_dependencies")
public class TaskDependency {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "schedule_id", length = 36, nullable = false)
    private String scheduleId;

    @Column(name = "predecessor_task_id", length = 36, nullable = false)
    private String predecessorTaskId;

    @Column(name = "successor_task_id", length = 36, nullable = false)
    private String successorTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", length = 10, nullable = false)
    private TaskDependencyType dependencyType;

    @Column(name = "lag_days", nullable = false)
    private int lagDays;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected TaskDependency() {
    }

    public TaskDependency(String scheduleId, String predecessorTaskId, String successorTaskId,
                          TaskDependencyType dependencyType, int lagDays) {
        this.id = UUID.randomUUID().toString();
        this.scheduleId = scheduleId;
        this.predecessorTaskId = predecessorTaskId;
        this.successorTaskId = successorTaskId;
        this.dependencyType = dependencyType != null ? dependencyType : TaskDependencyType.FS;
        this.lagDays = lagDays;
        this.createdAt = System.currentTimeMillis();
    }

    public void update(TaskDependencyType dependencyType, int lagDays) {
        if (dependencyType != null) {
            this.dependencyType = dependencyType;
        }
        this.lagDays = lagDays;
    }
}
