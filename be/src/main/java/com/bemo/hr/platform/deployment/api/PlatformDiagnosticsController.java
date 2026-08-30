package com.bemo.hr.platform.deployment.api;

import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.DiagnosticsResponse;
import com.bemo.hr.platform.deployment.application.PlatformDiagnosticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/diagnostics")
public class PlatformDiagnosticsController {

    private final PlatformDiagnosticsService diagnosticsService;

    public PlatformDiagnosticsController(PlatformDiagnosticsService diagnosticsService) {
        this.diagnosticsService = diagnosticsService;
    }

    @GetMapping("/health")
    @PreAuthorize("hasAuthority('P_SETTINGS_READ') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<DiagnosticsResponse> getDiagnostics() {
        DiagnosticsResponse response = diagnosticsService.evaluateAndRecordDiagnostics(System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAuthority('P_SETTINGS_MANAGE') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<DiagnosticsResponse> evaluateDiagnostics() {
        DiagnosticsResponse response = diagnosticsService.evaluateAndRecordDiagnostics(System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}
