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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE')")
    public List<WorkforceApi.LaborRequestResponse> list() {
        return requestService.list();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.LaborRequestResponse create(@Valid @RequestBody WorkforceApi.LaborRequestCreate request, Authentication auth) {
        String username = auth != null ? auth.getName() : "system";
        return requestService.create(request, username);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkforceApi.LaborRequestResponse updateStatus(@PathVariable String id, @RequestParam String status, Authentication auth) {
        String username = auth != null ? auth.getName() : "system";
        return requestService.updateStatus(id, status, username);
    }
}
