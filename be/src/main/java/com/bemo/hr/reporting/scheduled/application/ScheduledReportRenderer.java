package com.bemo.hr.reporting.scheduled.application;

import com.bemo.hr.reporting.application.DataExportService;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.reporting.scheduled.domain.ReportSchedule;
import com.bemo.hr.payroll.application.PayrollService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import tools.jackson.databind.ObjectMapper;

/**
 * Produces the actual report bytes for each schedulable {@link ReportSchedule.ReportKind}.
 * Kinds without a concrete export entry point fail loudly with {@code SCHED_RENDER_UNSUPPORTED}
 * instead of silently delivering empty content.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledReportRenderer {

    private final DataExportService dataExportService;
    private final PayrollService payrollService;
    private final ObjectMapper objectMapper;

    public byte[] render(ReportSchedule schedule) {
        ExcelExportOptions options = new ExcelExportOptions("ar-EG", null);
        switch (schedule.getReportKind()) {
            case TRENDS -> {
                return dataExportService.trends(intParam(schedule, "months", 12), options);
            }
            case ATTENDANCE -> {
                return dataExportService.clockInHistogram(
                        intParam(schedule, "months", 6),
                        strParam(schedule, "categoryId"),
                        options);
            }
            case PAYROLL -> {
                LocalDate now = LocalDate.now();
                int year = intParam(schedule, "year", now.getYear());
                int month = intParam(schedule, "month", now.getMonthValue());
                return payrollService.export(year, month, strParam(schedule, "categoryId"), options);
            }
            case AR_AGING, CASHFLOW, CUSTOM ->
                    throw new BusinessRuleException(
                            "Report kind " + schedule.getReportKind() + " cannot be rendered for scheduled delivery",
                            "SCHED_RENDER_UNSUPPORTED",
                            HttpStatus.NOT_IMPLEMENTED);
            default -> throw new BusinessRuleException(
                    "Unknown report kind " + schedule.getReportKind(),
                    "SCHED_RENDER_UNSUPPORTED",
                    HttpStatus.NOT_IMPLEMENTED);
        }
    }

    int intParam(ReportSchedule schedule, String key, int fallback) {
        try {
            var node = objectMapper.readTree(schedule.getParams() == null ? "{}" : schedule.getParams());
            var value = node.get(key);
            if (value == null || value.isNull()) return fallback;
            return value.isNumber() ? value.asInt() : fallback;
        } catch (Exception e) {
            log.debug("Param {} not an integer on schedule {}: {}", key, schedule.getId(), e.getMessage());
            return fallback;
        }
    }

    String strParam(ReportSchedule schedule, String key) {
        try {
            var node = objectMapper.readTree(schedule.getParams() == null ? "{}" : schedule.getParams());
            var value = node.get(key);
            return (value == null || value.isNull()) ? null : value.asText();
        } catch (Exception e) {
            log.debug("Param {} not readable on schedule {}: {}", key, schedule.getId(), e.getMessage());
            return null;
        }
    }
}