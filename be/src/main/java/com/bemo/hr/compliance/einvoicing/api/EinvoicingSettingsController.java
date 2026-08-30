package com.bemo.hr.compliance.einvoicing.api;

import com.bemo.hr.compliance.einvoicing.application.EinvoicingSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/einvoicing")
public class EinvoicingSettingsController {

    private final EinvoicingSettingsService service;

    public EinvoicingSettingsController(EinvoicingSettingsService service) {
        this.service = service;
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','FINANCE_MANAGER','ACCOUNTANT')")
    public ResponseEntity<EinvoicingApi.SettingsResponse> getSettings() {
        return service.getSettings().map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EinvoicingApi.SettingsResponse> saveSettings(@RequestBody @Valid EinvoicingApi.SaveSettingsRequest request) {
        return ResponseEntity.ok(service.saveSettings(request));
    }

    @GetMapping("/providers")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','FINANCE_MANAGER','ACCOUNTANT')")
    public ResponseEntity<List<EinvoicingApi.ProviderInfo>> listProviders() {
        return ResponseEntity.ok(service.listProviders());
    }
}
