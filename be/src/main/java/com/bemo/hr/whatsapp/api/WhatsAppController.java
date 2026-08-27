package com.bemo.hr.whatsapp.api;

import com.bemo.hr.whatsapp.application.WhatsAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class WhatsAppController {

    private final WhatsAppService service;

    @PostMapping("/test-send")
    @ResponseStatus(HttpStatus.CREATED)
    public WhatsAppApi.OutboundLogEntry testSend(@Valid @RequestBody WhatsAppApi.SendTestPayload payload) {
        return service.sendTest(payload.phoneNumber());
    }

    @GetMapping("/logs")
    public WhatsAppApi.LogResponse listLogs(org.springframework.security.core.Authentication auth,
                                            @RequestParam(defaultValue = "50") int limit) {
        return service.listLogs(resolveAppId(auth), limit);
    }

    @PostMapping("/resend")
    public void resend(@Valid @RequestBody WhatsAppApi.ResendPayload payload,
                       org.springframework.security.core.Authentication auth) {
        service.resend(resolveAppId(auth), payload.logId());
    }

    @PostMapping("/retry-failed")
    public void retryFailed(org.springframework.security.core.Authentication auth) {
        service.retryFailed(resolveAppId(auth));
    }

    @GetMapping("/settings")
    public WhatsAppApi.WhatsAppSettings getSettings() {
        return new WhatsAppApi.WhatsAppSettings(
                service.isConfigured(), service.isConfigured() ? "CLOUD_API" : "NONE",
                java.util.List.of(
                        new WhatsAppApi.TemplateMapping("payslip", "payslip_v1"),
                        new WhatsAppApi.TemplateMapping("invoice_overdue", "invoice_overdue_v1")));
    }

    @PostMapping("/status-webhook")
    public void statusWebhook(@RequestParam String providerMessageId, @RequestParam String status) {
        service.processStatusWebhook(providerMessageId, status);
    }

    private String resolveAppId(org.springframework.security.core.Authentication auth) {
        var details = auth.getDetails();
        if (details instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            return jwt.getClaimAsString("appId");
        }
        return "";
    }
}
