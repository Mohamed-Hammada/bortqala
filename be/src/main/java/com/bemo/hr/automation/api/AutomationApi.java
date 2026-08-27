package com.bemo.hr.automation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public final class AutomationApi {
    private AutomationApi() {}

    public record RecurringTemplatePayload(
            @NotBlank String kind, @NotBlank String templateName,
            @NotBlank String payloadSnapshot, @NotBlank String cadence,
            Integer cadenceDays, long nextRunAtEpochMs
    ) {}

    public record RecurringTemplateResponse(
            String id, String kind, String templateName, String payloadSnapshot,
            String cadence, Integer cadenceDays, long nextRunAtEpochMs,
            boolean active, String lastCreatedRef, Long version
    ) {}

    public record TemplateListResponse(List<RecurringTemplateResponse> templates) {}

    public record DunningRulePayload(
            @NotNull @Positive int daysOverdue, @NotBlank String templateKey, @NotBlank String channel
    ) {}

    public record DunningRuleResponse(
            String id, int daysOverdue, String templateKey, String channel, boolean active, Long version
    ) {}

    public record DunningRuleListResponse(List<DunningRuleResponse> rules) {}

    public record JobEntry(
            String id, String type, String status, String error, long createdAtEpochMs
    ) {}

    public record JobsHealthResponse(List<JobEntry> jobs, int total) {}

    public record RetryResponse(boolean success, String message) {}
}
