package com.bemo.hr.project.api;

import com.bemo.hr.project.api.ScheduleApi.*;
import com.bemo.hr.project.application.ProjectSchedulingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/schedule")
public class ProjectSchedulingController {

    private final ProjectSchedulingService schedulingService;

    public ProjectSchedulingController(ProjectSchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('projects.read', 'projects.manage')")
    public ProjectScheduleResponse getSchedule(@PathVariable String projectId) {
        return schedulingService.getSchedule(projectId);
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('projects.manage')")
    public ProjectScheduleTaskResponse createTask(
            @PathVariable String projectId,
            @Valid @RequestBody CreateScheduleTaskRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return schedulingService.createTask(projectId, req, userId);
    }

    @PutMapping("/tasks/{taskId}")
    @PreAuthorize("hasAuthority('projects.manage')")
    public ProjectScheduleTaskResponse updateTask(
            @PathVariable String projectId,
            @PathVariable String taskId,
            @Valid @RequestBody UpdateScheduleTaskRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return schedulingService.updateTask(projectId, taskId, req, userId);
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('projects.manage')")
    public void deleteTask(
            @PathVariable String projectId,
            @PathVariable String taskId,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        schedulingService.deleteTask(projectId, taskId, userId);
    }

    @PostMapping("/dependencies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('projects.manage')")
    public TaskDependencyResponse addDependency(
            @PathVariable String projectId,
            @Valid @RequestBody CreateDependencyRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return schedulingService.addDependency(projectId, req, userId);
    }

    @DeleteMapping("/dependencies/{dependencyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('projects.manage')")
    public void removeDependency(
            @PathVariable String projectId,
            @PathVariable String dependencyId,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        schedulingService.removeDependency(projectId, dependencyId, userId);
    }

    @PostMapping("/recalculate-cpm")
    @PreAuthorize("hasAuthority('projects.manage')")
    public ProjectScheduleResponse recalculateCpm(@PathVariable String projectId) {
        return schedulingService.recalculateCpm(projectId);
    }

    @PostMapping("/baselines")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('projects.manage')")
    public ScheduleBaselineResponse createBaseline(
            @PathVariable String projectId,
            @Valid @RequestBody CreateBaselineRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return schedulingService.createBaseline(projectId, req, userId);
    }

    @GetMapping("/baselines/{baselineId}/comparison")
    @PreAuthorize("hasAnyAuthority('projects.read', 'projects.manage')")
    public List<ScheduleBaselineComparisonResponse> getBaselineComparison(
            @PathVariable String projectId,
            @PathVariable String baselineId) {
        return schedulingService.getBaselineComparison(projectId, baselineId);
    }

    @PostMapping("/tasks/{taskId}/resources")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('projects.manage')")
    public TaskResourceAssignmentResponse assignResource(
            @PathVariable String projectId,
            @PathVariable String taskId,
            @Valid @RequestBody AssignResourceRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return schedulingService.assignResource(projectId, taskId, req, userId);
    }

    @DeleteMapping("/resources/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('projects.manage')")
    public void removeResourceAssignment(
            @PathVariable String projectId,
            @PathVariable String assignmentId,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        schedulingService.removeResourceAssignment(projectId, assignmentId, userId);
    }

    @GetMapping("/resources/over-allocations")
    @PreAuthorize("hasAnyAuthority('projects.read', 'projects.manage')")
    public List<ResourceOverAllocationResponse> detectOverAllocations(@PathVariable String projectId) {
        return schedulingService.detectOverAllocations(projectId);
    }

    @PostMapping("/import-wbs")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('projects.manage')")
    public ProjectScheduleResponse importFromWbs(
            @PathVariable String projectId,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        schedulingService.importFromWbs(projectId, userId);
        return schedulingService.getSchedule(projectId);
    }

    @PostMapping("/sync-dpr")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('projects.manage')")
    public ProjectScheduleResponse syncProgressFromDpr(
            @PathVariable String projectId,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        schedulingService.syncProgressFromDpr(projectId, userId);
        return schedulingService.getSchedule(projectId);
    }
}
