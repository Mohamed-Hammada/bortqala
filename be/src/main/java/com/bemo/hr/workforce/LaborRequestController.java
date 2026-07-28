package com.bemo.hr.workforce;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/labor-requests")
@RequiredArgsConstructor
public class LaborRequestController {
    private final LaborRequestService requestService;

    @GetMapping
    public List<WorkforceApi.LaborRequestResponse> list() {
        return requestService.list();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.LaborRequestResponse create(@Valid @RequestBody WorkforceApi.LaborRequestCreate request, Authentication auth) {
        String username = auth != null ? auth.getName() : "system";
        return requestService.create(request, username);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public WorkforceApi.LaborRequestResponse updateStatus(@PathVariable String id, @RequestParam String status, Authentication auth) {
        String username = auth != null ? auth.getName() : "system";
        return requestService.updateStatus(id, status, username);
    }
}
