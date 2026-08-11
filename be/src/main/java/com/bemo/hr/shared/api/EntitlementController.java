package com.bemo.hr.shared.api;
import com.bemo.hr.shared.security.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1/platform/entitlements") @RequiredArgsConstructor @PreAuthorize("hasRole('SUPER_ADMIN')")
public class EntitlementController {private final EntitlementManagementService service;
    @GetMapping List<EntitlementApi.ModuleResponse> catalog(){return service.catalog(TenantContext.require());}
    @PutMapping("/{featureKey}") EntitlementApi.FeatureResponse update(@PathVariable String featureKey,@Valid @RequestBody EntitlementApi.UpdateRequest request, Authentication auth){return service.update(TenantContext.require(),featureKey,request,auth.getName());}}
