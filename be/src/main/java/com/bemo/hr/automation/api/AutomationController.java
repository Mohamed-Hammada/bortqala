package com.bemo.hr.automation.api;

import com.bemo.hr.automation.application.AutomationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/automation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AutomationController {

    private final AutomationService service;

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public AutomationApi.RecurringTemplateResponse createTemplate(
            @Valid @RequestBody AutomationApi.RecurringTemplatePayload payload,
            org.springframework.security.core.Authentication auth) {
        return service.createTemplate(resolveAppId(auth), payload);
    }

    @GetMapping("/templates")
    public AutomationApi.TemplateListResponse listTemplates(org.springframework.security.core.Authentication auth) {
        return service.listTemplates(resolveAppId(auth));
    }

    @PostMapping("/templates/{id}/toggle")
    public void toggleTemplate(@PathVariable String id, @RequestParam boolean active,
                               org.springframework.security.core.Authentication auth) {
        service.toggleTemplate(resolveAppId(auth), id, active);
    }

    @PostMapping("/templates/run")
    public java.util.Map<String, Object> runTemplates(org.springframework.security.core.Authentication auth) {
        int created = service.runRecurringTemplates(resolveAppId(auth));
        return java.util.Map.of("created", created);
    }

    @PostMapping("/dunning-rules")
    @ResponseStatus(HttpStatus.CREATED)
    public AutomationApi.DunningRuleResponse createDunningRule(
            @Valid @RequestBody AutomationApi.DunningRulePayload payload,
            org.springframework.security.core.Authentication auth) {
        return service.createDunningRule(resolveAppId(auth), payload);
    }

    @GetMapping("/dunning-rules")
    public AutomationApi.DunningRuleListResponse listDunningRules(org.springframework.security.core.Authentication auth) {
        return service.listDunningRules(resolveAppId(auth));
    }

    @PostMapping("/dunning-rules/{id}/toggle")
    public void toggleDunningRule(@PathVariable String id, @RequestParam boolean active,
                                  org.springframework.security.core.Authentication auth) {
        service.toggleDunningRule(resolveAppId(auth), id, active);
    }

    @PostMapping("/dunning/run")
    public java.util.Map<String, Object> runDunning(org.springframework.security.core.Authentication auth) {
        int evaluated = service.runDunning(resolveAppId(auth));
        return java.util.Map.of("evaluated", evaluated);
    }

    @GetMapping("/jobs")
    public AutomationApi.JobsHealthResponse getJobs(
            org.springframework.security.core.Authentication auth,
            @RequestParam(required = false) String status) {
        return service.getJobsHealth(resolveAppId(auth), status);
    }

    private String resolveAppId(org.springframework.security.core.Authentication auth) {
        var details = auth.getDetails();
        if (details instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            return jwt.getClaimAsString("appId");
        }
        return "";
    }
}
