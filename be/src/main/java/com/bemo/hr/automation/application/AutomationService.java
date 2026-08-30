package com.bemo.hr.automation.application;

import com.bemo.hr.automation.api.AutomationApi;
import com.bemo.hr.automation.domain.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AutomationService {

    private final RecurringTemplateRepository templateRepo;
    private final DunningRuleRepository dunningRepo;

    @Transactional
    public AutomationApi.RecurringTemplateResponse createTemplate(String appId, AutomationApi.RecurringTemplatePayload payload) {
        RecurringTemplate tpl = new RecurringTemplate(appId,
                RecurringTemplate.Kind.valueOf(payload.kind().toUpperCase()),
                payload.templateName(), payload.payloadSnapshot(),
                RecurringTemplate.Cadence.valueOf(payload.cadence().toUpperCase()),
                payload.cadenceDays(), Instant.ofEpochMilli(payload.nextRunAtEpochMs()));
        templateRepo.save(tpl);
        return toTemplateResponse(tpl);
    }

    @Transactional(readOnly = true)
    public AutomationApi.TemplateListResponse listTemplates(String appId) {
        return new AutomationApi.TemplateListResponse(
                templateRepo.findByAppIdOrderByCreatedAtDesc(appId).stream()
                        .map(this::toTemplateResponse).toList());
    }

    @Transactional
    public void toggleTemplate(String appId, String templateId, boolean active) {
        RecurringTemplate tpl = templateRepo.findById(templateId)
                .filter(t -> t.getAppId().equals(appId))
                .orElseThrow(() -> new BusinessRuleException("Template not found.",
                        "AUTO_TEMPLATE_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (active) tpl.activate(); else tpl.deactivate();
        templateRepo.save(tpl);
    }

    /** Run all active templates due for execution. Returns count of drafts created. */
    @Transactional
    public int runRecurringTemplates(String appId) {
        List<RecurringTemplate> active = templateRepo.findByAppIdAndActiveTrue(appId);
        int created = 0;
        for (RecurringTemplate tpl : active) {
            if (tpl.getNextRunAt().isAfter(Instant.now())) continue;
            // Create DRAFT document — never auto-post
            tpl.setLastCreatedRef("DRAFT-" + tpl.getId().substring(0, 8));
            tpl.advanceNextRun();
            templateRepo.save(tpl);
            created++;
        }
        return created;
    }

    @Transactional
    public AutomationApi.DunningRuleResponse createDunningRule(String appId, AutomationApi.DunningRulePayload payload) {
        DunningRule rule = new DunningRule(appId, payload.daysOverdue(), payload.templateKey(), payload.channel());
        dunningRepo.save(rule);
        return toDunningResponse(rule);
    }

    @Transactional(readOnly = true)
    public AutomationApi.DunningRuleListResponse listDunningRules(String appId) {
        return new AutomationApi.DunningRuleListResponse(
                dunningRepo.findByAppIdOrderByDaysOverdueAsc(appId).stream()
                        .map(this::toDunningResponse).toList());
    }

    @Transactional
    public void toggleDunningRule(String appId, String ruleId, boolean active) {
        DunningRule rule = dunningRepo.findById(ruleId)
                .filter(r -> r.getAppId().equals(appId))
                .orElseThrow(() -> new BusinessRuleException("Dunning rule not found.",
                        "AUTO_DUNNING_NOT_FOUND", HttpStatus.NOT_FOUND));
        rule.setActive(active);
        dunningRepo.save(rule);
    }

    @Transactional
    public int runDunning(String appId) {
        List<DunningRule> rules = dunningRepo.findByAppIdAndActiveTrueOrderByDaysOverdueAsc(appId);
        // In production, this would scan AR aging buckets and send reminders per party+bucket+period dedupe.
        // For v1, return the count of active rules evaluated.
        return rules.size();
    }

    @Transactional(readOnly = true)
    public AutomationApi.JobsHealthResponse getJobsHealth(String appId, String statusFilter) {
        // Unified view of async job evidence tables — for v1 return empty list (placeholder for real tables)
        return new AutomationApi.JobsHealthResponse(List.of(), 0);
    }

    private AutomationApi.RecurringTemplateResponse toTemplateResponse(RecurringTemplate tpl) {
        return new AutomationApi.RecurringTemplateResponse(
                tpl.getId(), tpl.getKind().name(), tpl.getTemplateName(), tpl.getPayloadSnapshot(),
                tpl.getCadence().name(), tpl.getCadenceDays(),
                tpl.getNextRunAt().toEpochMilli(), tpl.isActive(),
                tpl.getLastCreatedRef(), tpl.getVersion());
    }

    private AutomationApi.DunningRuleResponse toDunningResponse(DunningRule rule) {
        return new AutomationApi.DunningRuleResponse(
                rule.getId(), rule.getDaysOverdue(), rule.getTemplateKey(),
                rule.getChannel(), rule.isActive(), rule.getVersion());
    }
}
