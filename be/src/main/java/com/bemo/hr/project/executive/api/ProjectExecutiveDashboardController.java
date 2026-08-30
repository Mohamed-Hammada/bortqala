package com.bemo.hr.project.executive.api;

import com.bemo.hr.project.executive.api.ProjectExecutiveDashboardApi.ProjectExecutiveDashboardResponse;
import com.bemo.hr.project.executive.application.ProjectExecutiveDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/executive-dashboard/projects")
public class ProjectExecutiveDashboardController {

    private final ProjectExecutiveDashboardService executiveDashboardService;

    public ProjectExecutiveDashboardController(ProjectExecutiveDashboardService executiveDashboardService) {
        this.executiveDashboardService = executiveDashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('projects.read', 'projects.manage', 'finance.read', 'platform.admin')")
    public ProjectExecutiveDashboardResponse getExecutiveDashboard(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String branchId,
            Authentication auth) {
        boolean canViewTreasury = false;
        if (auth != null) {
            canViewTreasury = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals("finance.read") || a.equals("finance.manage") || a.equals("ROLE_SUPER_ADMIN"));
        }

        return executiveDashboardService.getExecutiveDashboard(companyId, branchId, canViewTreasury);
    }
}
