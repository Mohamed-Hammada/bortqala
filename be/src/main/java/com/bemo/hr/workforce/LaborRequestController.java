package com.bemo.hr.workforce;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/labor-requests")
@RequiredArgsConstructor
public class LaborRequestController {
    private final LaborRequestService requestService;

    @GetMapping
    @PreAuthorize("@auth.hasPermission('laborRequests.read')")
    public List<WorkforceApi.LaborRequestResponse> list() {
        return requestService.list();
    }

    @PostMapping
    @PreAuthorize("@auth.hasPermission('laborRequests.manage')")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.LaborRequestResponse create(@Valid @RequestBody WorkforceApi.LaborRequestCreate request, Authentication auth) {
        String username = auth != null ? auth.getName() : "system";
        return requestService.create(request, username);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@auth.hasPermission('laborRequests.manage')")
    public WorkforceApi.LaborRequestResponse updateStatus(@PathVariable String id, @RequestParam String status, Authentication auth) {
        String username = auth != null ? auth.getName() : "system";
        return requestService.updateStatus(id, status, username);
    }
}
