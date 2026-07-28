package com.bemo.hr.workforce;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/advances")
@RequiredArgsConstructor
public class WorkforceAdvanceController {
    private final WorkforceAdvanceService advanceService;

    @GetMapping
    public List<WorkforceApi.AdvanceResponse> list() {
        return advanceService.list();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.AdvanceResponse create(@Valid @RequestBody WorkforceApi.AdvanceCreateRequest request, Authentication auth) {
        String username = auth != null ? auth.getName() : "system";
        return advanceService.create(request, username);
    }
}
