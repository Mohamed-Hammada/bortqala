package com.bemo.hr.project.api;

import com.bemo.hr.project.api.ProjectApi.*;
import com.bemo.hr.project.application.SiteCustodyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
public class SiteCustodyController {

    private final SiteCustodyService custodyService;

    public SiteCustodyController(SiteCustodyService custodyService) {
        this.custodyService = custodyService;
    }

    @PostMapping("/{projectId}/custodies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('projects.manage') or hasRole('ADMIN')")
    public SiteCustodyResponse issueCustody(
            @PathVariable String projectId,
            @Valid @RequestBody IssueCustodyRequest req) {
        return custodyService.issueCustody(projectId, req);
    }

    @GetMapping("/{projectId}/custodies")
    @PreAuthorize("@auth.hasPermission('projects.read') or hasRole('ADMIN')")
    public List<SiteCustodyResponse> getCustodiesByProject(@PathVariable String projectId) {
        return custodyService.getCustodiesByProject(projectId);
    }

    @GetMapping("/custodies/{custodyId}")
    @PreAuthorize("@auth.hasPermission('projects.read') or hasRole('ADMIN')")
    public SiteCustodyResponse getCustody(@PathVariable String custodyId) {
        return custodyService.getCustody(custodyId);
    }

    @PostMapping("/custodies/{custodyId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('projects.manage') or hasRole('ADMIN')")
    public SiteCustodyExpenseResponse recordExpense(
            @PathVariable String custodyId,
            @Valid @RequestBody RecordCustodyExpenseRequest req) {
        return custodyService.recordExpense(custodyId, req);
    }

    @PostMapping("/custodies/expenses/{expenseId}/approve")
    @PreAuthorize("@auth.hasPermission('projects.manage') or hasRole('ADMIN')")
    public SiteCustodyExpenseResponse approveExpense(@PathVariable String expenseId) {
        return custodyService.approveExpense(expenseId);
    }

    @PostMapping("/custodies/expenses/{expenseId}/reject")
    @PreAuthorize("@auth.hasPermission('projects.manage') or hasRole('ADMIN')")
    public SiteCustodyExpenseResponse rejectExpense(@PathVariable String expenseId) {
        return custodyService.rejectExpense(expenseId);
    }

    @PostMapping("/custodies/{custodyId}/settle")
    @PreAuthorize("@auth.hasPermission('projects.manage') or hasRole('ADMIN')")
    public SiteCustodyResponse settleCustody(
            @PathVariable String custodyId,
            @Valid @RequestBody SettleCustodyRequest req) {
        return custodyService.settleCustody(custodyId, req);
    }
}
