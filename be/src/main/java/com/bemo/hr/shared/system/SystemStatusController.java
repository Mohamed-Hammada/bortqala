package com.bemo.hr.shared.system;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SystemStatusController {
    private final SystemStatusService systemStatusService;

    public SystemStatusController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    @GetMapping("/system/status")
    SystemStatusApi.StatusResponse status() {
        return systemStatusService.status();
    }

    @PostMapping("/admin/system/cache-version")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    SystemStatusApi.StatusResponse rotateCacheVersion(
            @Valid @RequestBody(required = false) SystemStatusApi.RotateCacheRequest request,
            Authentication authentication) {
        return systemStatusService.rotateCacheVersion(
                authentication.getName(),
                request == null ? null : request.reason());
    }
}
