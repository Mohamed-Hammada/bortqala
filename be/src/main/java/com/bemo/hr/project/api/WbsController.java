package com.bemo.hr.project.api;

import com.bemo.hr.project.api.ProjectApi.*;
import com.bemo.hr.project.application.WbsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/wbs")
public class WbsController {

    private final WbsService wbsService;

    public WbsController(WbsService wbsService) {
        this.wbsService = wbsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROJECT_MANAGER', 'FINANCE_MANAGER', 'AUDITOR', 'VIEWER')")
    public List<WbsNodeResponse> getWbsTree(@PathVariable String projectId) {
        return wbsService.getWbsTree(projectId);
    }

    @GetMapping("/flat")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROJECT_MANAGER', 'FINANCE_MANAGER', 'AUDITOR', 'VIEWER')")
    public List<WbsNodeResponse> getFlatWbsList(@PathVariable String projectId) {
        return wbsService.getFlatWbsList(projectId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROJECT_MANAGER', 'FINANCE_MANAGER', 'AUDITOR', 'VIEWER')")
    public WbsNodeResponse getWbsNode(
            @PathVariable String projectId,
            @PathVariable String id
    ) {
        return wbsService.getWbsNode(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROJECT_MANAGER')")
    public WbsNodeResponse createWbsNode(
            @PathVariable String projectId,
            @Valid @RequestBody CreateWbsNodeRequest request
    ) {
        return wbsService.createWbsNode(projectId, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROJECT_MANAGER')")
    public WbsNodeResponse updateWbsNode(
            @PathVariable String projectId,
            @PathVariable String id,
            @Valid @RequestBody UpdateWbsNodeRequest request
    ) {
        return wbsService.updateWbsNode(id, request);
    }

    @PostMapping("/{id}/reposition")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROJECT_MANAGER')")
    public WbsNodeResponse repositionWbsNode(
            @PathVariable String projectId,
            @PathVariable String id,
            @Valid @RequestBody RepositionWbsNodeRequest request
    ) {
        return wbsService.repositionWbsNode(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROJECT_MANAGER')")
    public void deleteWbsNode(
            @PathVariable String projectId,
            @PathVariable String id
    ) {
        wbsService.deleteWbsNode(id);
    }
}
