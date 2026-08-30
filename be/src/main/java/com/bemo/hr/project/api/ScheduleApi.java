package com.bemo.hr.project.api;

import com.bemo.hr.project.domain.ScheduleStatus;
import com.bemo.hr.project.domain.TaskConstraintType;
import com.bemo.hr.project.domain.TaskDependencyType;
import com.bemo.hr.project.domain.TaskResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class ScheduleApi {

    private ScheduleApi() {
    }

    public record ProjectScheduleResponse(
            String id,
            String projectId,
            String name,
            String calendarCode,
            Long startDate,
            Long endDate,
            ScheduleStatus status,
            int currentBaselineVersion,
            int totalTasksCount,
            int criticalTasksCount,
            BigDecimal overallProgress,
            long createdAt,
            long updatedAt,
            long version,
            List<ProjectScheduleTaskResponse> tasks,
            List<TaskDependencyResponse> dependencies,
            List<ScheduleBaselineResponse> baselines
    ) {}

    public record ProjectScheduleTaskResponse(
            String id,
            String scheduleId,
            String wbsNodeId,
            String parentTaskId,
            String taskCode,
            String name,
            String nameEn,
            int durationDays,
            Long plannedStartDate,
            Long plannedEndDate,
            Long earlyStartDate,
            Long earlyEndDate,
            Long lateStartDate,
            Long lateEndDate,
            int freeFloatDays,
            int totalFloatDays,
            boolean isCritical,
            BigDecimal percentComplete,
            boolean isMilestone,
            TaskConstraintType constraintType,
            Long constraintDate,
            int sortOrder,
            List<TaskResourceAssignmentResponse> resourceAssignments
    ) {}

    public record TaskDependencyResponse(
            String id,
            String scheduleId,
            String predecessorTaskId,
            String successorTaskId,
            TaskDependencyType dependencyType,
            int lagDays,
            long createdAt
    ) {}

    public record ScheduleBaselineResponse(
            String id,
            String scheduleId,
            int versionNumber,
            String name,
            String approvedBy,
            Long approvedAt,
            String notes,
            int taskCount
    ) {}

    public record ScheduleBaselineComparisonResponse(
            String taskId,
            String taskCode,
            String taskName,
            Long baselineStartDate,
            Long baselineEndDate,
            int baselineDurationDays,
            Long currentStartDate,
            Long currentEndDate,
            int currentDurationDays,
            int varianceDays,
            boolean isCritical
    ) {}

    public record TaskResourceAssignmentResponse(
            String id,
            String taskId,
            TaskResourceType resourceType,
            String resourceName,
            String partyId,
            String employeeId,
            BigDecimal quantityAllocated,
            Long startDate,
            Long endDate,
            String notes
    ) {}

    public record ResourceOverAllocationResponse(
            TaskResourceType resourceType,
            String resourceName,
            Long date,
            BigDecimal allocatedQuantity,
            BigDecimal capacityLimit,
            BigDecimal overAllocatedAmount,
            List<String> affectedTaskCodes
    ) {}

    public record CreateScheduleTaskRequest(
            String wbsNodeId,
            String parentTaskId,
            @NotBlank String taskCode,
            @NotBlank String name,
            String nameEn,
            int durationDays,
            Long plannedStartDate,
            Long plannedEndDate,
            boolean isMilestone,
            TaskConstraintType constraintType,
            Long constraintDate,
            int sortOrder
    ) {}

    public record UpdateScheduleTaskRequest(
            String wbsNodeId,
            String parentTaskId,
            @NotBlank String taskCode,
            @NotBlank String name,
            String nameEn,
            int durationDays,
            Long plannedStartDate,
            Long plannedEndDate,
            boolean isMilestone,
            TaskConstraintType constraintType,
            Long constraintDate,
            int sortOrder
    ) {}

    public record CreateDependencyRequest(
            @NotBlank String predecessorTaskId,
            @NotBlank String successorTaskId,
            @NotNull TaskDependencyType dependencyType,
            int lagDays
    ) {}

    public record CreateBaselineRequest(
            @NotBlank String name,
            String notes
    ) {}

    public record AssignResourceRequest(
            @NotNull TaskResourceType resourceType,
            @NotBlank String resourceName,
            String partyId,
            String employeeId,
            @NotNull BigDecimal quantityAllocated,
            Long startDate,
            Long endDate,
            String notes
    ) {}
}
