package com.bemo.hr.finance.paylink.api;

import com.bemo.hr.finance.paylink.application.PaymentLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/payment-links")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PaymentLinkController {

    private final PaymentLinkService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT')")
    public PaylinkApi.LinkResponse create(@Valid @RequestBody PaylinkApi.CreateLinkPayload payload,
                                          org.springframework.security.core.Authentication auth) {
        return service.createLink(resolveAppId(auth), payload, "Bemo ERP");
    }

    @GetMapping
    public List<PaylinkApi.LinkResponse> list(org.springframework.security.core.Authentication auth) {
        return service.listLinks(resolveAppId(auth));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public void cancel(@PathVariable String id, org.springframework.security.core.Authentication auth) {
        service.cancelLink(resolveAppId(auth), id);
    }

    @GetMapping("/config")
    public java.util.Map<String, Object> config() {
        return java.util.Map.of("enabled", service.isGatewayEnabled());
    }

    private String resolveAppId(org.springframework.security.core.Authentication auth) {
        var details = auth.getDetails();
        if (details instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            return jwt.getClaimAsString("appId");
        }
        return "";
    }
}
