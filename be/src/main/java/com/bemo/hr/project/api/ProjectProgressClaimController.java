package com.bemo.hr.project.api;

import com.bemo.hr.project.api.ClaimApi.*;
import com.bemo.hr.project.application.ProjectProgressClaimService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProjectProgressClaimController {

    private final ProjectProgressClaimService claimService;

    public ProjectProgressClaimController(ProjectProgressClaimService claimService) {
        this.claimService = claimService;
    }

    @GetMapping("/api/v1/projects/{projectId}/claims")
    @PreAuthorize("@auth.hasAnyPermission('projects.read', 'projects.manage')")
    public List<ProjectProgressClaimResponse> listClaimsForProject(@PathVariable String projectId) {
        return claimService.listClaimsForProject(projectId);
    }

    @PostMapping("/api/v1/projects/{projectId}/claims")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public ProjectProgressClaimResponse createClaim(
            @PathVariable String projectId,
            @Valid @RequestBody CreateProgressClaimRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        CreateProgressClaimRequest payload = new CreateProgressClaimRequest(
                req.claimType(),
                req.claimKind(),
                projectId,
                req.partyId(),
                req.periodStartDate(),
                req.periodEndDate(),
                req.currencyCode(),
                req.notes(),
                req.initFromWbs()
        );
        return claimService.createClaim(payload, userId);
    }

    @GetMapping("/api/v1/project-claims/{id}")
    @PreAuthorize("@auth.hasAnyPermission('projects.read', 'projects.manage')")
    public ProjectProgressClaimResponse getClaim(@PathVariable String id) {
        return claimService.getClaim(id);
    }

    @PutMapping("/api/v1/project-claims/{id}")
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public ProjectProgressClaimResponse updateDraftClaim(
            @PathVariable String id,
            @Valid @RequestBody UpdateProgressClaimRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return claimService.updateDraftClaim(id, req, userId);
    }

    @DeleteMapping("/api/v1/project-claims/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public void deleteDraftClaim(@PathVariable String id, Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        claimService.deleteDraftClaim(id, userId);
    }

    @PostMapping("/api/v1/project-claims/{id}/submit")
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public ProjectProgressClaimResponse submitClaim(@PathVariable String id, Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return claimService.submitClaim(id, userId);
    }

    @PostMapping("/api/v1/project-claims/{id}/review")
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public ProjectProgressClaimResponse reviewClaim(@PathVariable String id, Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return claimService.reviewClaim(id, userId);
    }

    @PostMapping("/api/v1/project-claims/{id}/certify")
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public ProjectProgressClaimResponse certifyClaim(
            @PathVariable String id,
            @RequestBody(required = false) CertifyClaimRequest req,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return claimService.certifyClaim(id, req, userId);
    }

    @PostMapping("/api/v1/project-claims/{id}/post-finance")
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public ProjectProgressClaimResponse postClaimToFinance(@PathVariable String id, Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return claimService.postClaimToFinance(id, userId);
    }

    @PostMapping("/api/v1/project-claims/{id}/cancel")
    @PreAuthorize("@auth.hasPermission('projects.manage')")
    public ProjectProgressClaimResponse cancelClaim(@PathVariable String id, Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return claimService.cancelClaim(id, userId);
    }
}
