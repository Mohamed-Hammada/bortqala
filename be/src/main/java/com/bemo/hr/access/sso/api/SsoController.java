package com.bemo.hr.access.sso.api;

import com.bemo.hr.access.sso.application.SsoApi;
import com.bemo.hr.access.sso.application.SsoService;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/sso")
@RequiredArgsConstructor
public class SsoController {

    private final SsoService ssoService;

    @GetMapping("/probe")
    ResponseEntity<SsoApi.ProbeResponse> probe() {
        boolean hasGoogle = ssoService.hasActiveConfig("GOOGLE");
        boolean hasMicrosoft = ssoService.hasActiveConfig("MICROSOFT");
        return ResponseEntity.ok(new SsoApi.ProbeResponse(hasGoogle, hasMicrosoft));
    }

    @GetMapping("/{provider}/start")
    ResponseEntity<SsoApi.StartResponse> start(@PathVariable String provider) {
        return ResponseEntity.ok(ssoService.startAuth(provider));
    }

    @GetMapping("/callback")
    ResponseEntity<SsoApi.CallbackResult> callback(
            @RequestParam String state,
            @RequestParam(defaultValue = "google") String provider,
            @RequestParam String code) {
        return ResponseEntity.ok(ssoService.handleCallback(state, provider, code));
    }

    @GetMapping("/configs")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    ResponseEntity<List<SsoApi.ConfigResponse>> listConfigs() {
        return ResponseEntity.ok(ssoService.listConfigs().stream()
                .map(SsoApi.ConfigResponse::from).toList());
    }

    @PostMapping("/configs")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    ResponseEntity<SsoApi.ConfigResponse> createConfig(@Valid @RequestBody SsoApi.CreateConfigRequest request) {
        return ResponseEntity.ok(SsoApi.ConfigResponse.from(ssoService.createConfig(request)));
    }

    @PutMapping("/configs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    ResponseEntity<SsoApi.ConfigResponse> updateConfig(@PathVariable String id,
                                                       @Valid @RequestBody SsoApi.UpdateConfigRequest request) {
        return ResponseEntity.ok(SsoApi.ConfigResponse.from(ssoService.updateConfig(id, request)));
    }

    @DeleteMapping("/configs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    ResponseEntity<Void> deleteConfig(@PathVariable String id) {
        ssoService.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/identities/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    ResponseEntity<List<SsoApi.IdentityResponse>> getUserIdentities(@PathVariable String userId) {
        return ResponseEntity.ok(ssoService.getUserIdentities(userId));
    }
}
