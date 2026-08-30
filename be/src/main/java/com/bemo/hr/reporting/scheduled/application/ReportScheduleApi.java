package com.bemo.hr.reporting.scheduled.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public interface ReportScheduleApi {

    record CreateRequest(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "ATTENDANCE|PAYROLL|AR_AGING|CASHFLOW|TRENDS|CUSTOM") String reportKind,
            String params,
            @NotBlank @Pattern(regexp = "EMAIL|WHATSAPP") String channel,
            String recipients,
            @NotBlank @Pattern(regexp = "DAILY|WEEKLY|MONTHLY") String cadence,
            @Pattern(regexp = "\\d{2}:\\d{2}") String timeOfDay
    ) {}

    record UpdateRequest(
            String name,
            @Pattern(regexp = "ATTENDANCE|PAYROLL|AR_AGING|CASHFLOW|TRENDS|CUSTOM") String reportKind,
            String params,
            @Pattern(regexp = "EMAIL|WHATSAPP") String channel,
            String recipients,
            @Pattern(regexp = "DAILY|WEEKLY|MONTHLY") String cadence,
            @Pattern(regexp = "\\d{2}:\\d{2}") String timeOfDay,
            Boolean active
    ) {}

    record Response(
            String id,
            String name,
            String reportKind,
            String params,
            String channel,
            String recipients,
            String cadence,
            String timeOfDay,
            boolean active,
            Instant lastRunAt,
            String lastStatus,
            String lastError,
            int consecutiveFailures,
            Long version
    ) {
        public static Response from(com.bemo.hr.reporting.scheduled.domain.ReportSchedule s) {
            return new Response(
                    s.getId(), s.getName(), s.getReportKind().name(), s.getParams(),
                    s.getChannel().name(), s.getRecipients(), s.getCadence().name(),
                    s.getTimeOfDay(), s.isActive(), s.getLastRunAt(), s.getLastStatus(),
                    s.getLastError(), s.getConsecutiveFailures(), s.getVersion()
            );
        }
    }
}
