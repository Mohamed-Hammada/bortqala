package com.bemo.hr.project.api;

import com.bemo.hr.project.api.ProjectApi.*;
import com.bemo.hr.project.application.ProjectService;
import com.bemo.hr.project.domain.ProjectStatus;
import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @PreAuthorize(Roles.ADMIN_AUDITOR_FINANCE_MANAGER_PROJECT_MANAGER_VIEWER)
    public List<ProjectResponse> listProjects(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) ProjectStatus status
    ) {
        return projectService.listProjects(companyId, status);
    }

    @GetMapping("/summary")
    @PreAuthorize(Roles.ADMIN_AUDITOR_FINANCE_MANAGER_PROJECT_MANAGER_VIEWER)
    public ProjectSummaryResponse getProjectSummary() {
        return projectService.getProjectSummary();
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_AUDITOR_FINANCE_MANAGER_PROJECT_MANAGER_VIEWER)
    public ProjectResponse getProject(@PathVariable String id) {
        return projectService.getProject(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN_PROJECT_MANAGER)
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_PROJECT_MANAGER)
    public ProjectResponse updateProject(
            @PathVariable String id,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return projectService.updateProject(id, request);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize(Roles.ADMIN_PROJECT_MANAGER)
    public ProjectResponse activateProject(@PathVariable String id) {
        return projectService.activateProject(id);
    }

    @PostMapping("/{id}/hold")
    @PreAuthorize(Roles.ADMIN_PROJECT_MANAGER)
    public ProjectResponse holdProject(@PathVariable String id) {
        return projectService.holdProject(id);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize(Roles.ADMIN_PROJECT_MANAGER)
    public ProjectResponse completeProject(@PathVariable String id) {
        return projectService.completeProject(id);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize(Roles.ADMIN_PROJECT_MANAGER)
    public ProjectResponse closeProject(@PathVariable String id) {
        return projectService.closeProject(id);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize(Roles.ADMIN_PROJECT_MANAGER)
    public ProjectResponse reopenProject(@PathVariable String id) {
        return projectService.reopenProject(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.ADMIN_PROJECT_MANAGER)
    public void deleteProject(@PathVariable String id) {
        projectService.deleteProject(id);
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize(Roles.ADMIN_AUDITOR_FINANCE_MANAGER_PROJECT_MANAGER_VIEWER)
    public List<ProjectPartyRoleResponse> getProjectRoles(@PathVariable String id) {
        return projectService.getProjectRoles(id);
    }

    @PostMapping("/{id}/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN_PROJECT_MANAGER)
    public ProjectPartyRoleResponse assignProjectRole(
            @PathVariable String id,
            @Valid @RequestBody AssignPartyRoleRequest request
    ) {
        return projectService.assignProjectRole(id, request);
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.ADMIN_PROJECT_MANAGER)
    public void removeProjectRole(
            @PathVariable String id,
            @PathVariable String roleId
    ) {
        projectService.removeProjectRole(roleId);
    }
}
