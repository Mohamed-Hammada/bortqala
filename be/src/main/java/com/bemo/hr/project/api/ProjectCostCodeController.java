package com.bemo.hr.project.api;

import com.bemo.hr.project.api.ProjectApi.*;
import com.bemo.hr.project.application.ProjectCostCodeService;
import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/cost-codes")
public class ProjectCostCodeController {

    private final ProjectCostCodeService projectCostCodeService;

    public ProjectCostCodeController(ProjectCostCodeService projectCostCodeService) {
        this.projectCostCodeService = projectCostCodeService;
    }

    @GetMapping
    @PreAuthorize(Roles.ADMIN_AUDITOR_FINANCE_MANAGER_PROJECT_MANAGER_VIEWER)
    public List<ProjectCostCodeResponse> listCostCodes(@RequestParam(required = false) Boolean activeOnly) {
        return projectCostCodeService.listCostCodes(activeOnly);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_AUDITOR_FINANCE_MANAGER_PROJECT_MANAGER_VIEWER)
    public ProjectCostCodeResponse getCostCode(@PathVariable String id) {
        return projectCostCodeService.getCostCode(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN_PROJECT_MANAGER)
    public ProjectCostCodeResponse createCostCode(@Valid @RequestBody CreateCostCodeRequest request) {
        return projectCostCodeService.createCostCode(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_PROJECT_MANAGER)
    public ProjectCostCodeResponse updateCostCode(
            @PathVariable String id,
            @Valid @RequestBody UpdateCostCodeRequest request
    ) {
        return projectCostCodeService.updateCostCode(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.ADMIN_PROJECT_MANAGER)
    public void deleteCostCode(@PathVariable String id) {
        projectCostCodeService.deleteCostCode(id);
    }
}
