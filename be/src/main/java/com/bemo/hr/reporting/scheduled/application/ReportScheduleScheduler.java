package com.bemo.hr.reporting.scheduled.application;

import com.bemo.hr.reporting.scheduled.domain.ReportSchedule;
import com.bemo.hr.reporting.scheduled.domain.ReportScheduleRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Per-tenant poller that runs every minute and executes active schedules whose
 * cadence + time-of-day window has arrived.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportScheduleScheduler {

    private final TenantApplicationRepository tenantApplicationRepository;
    private final ReportScheduleRepository reportScheduleRepository;
    private final ReportScheduleService reportScheduleService;

    @Scheduled(cron = "${hr.schedules.poll-cron:0 * * * * *}")
    public void runDueSchedules() {
        for (TenantApplication app : tenantApplicationRepository.findAll()) {
            TenantContext.set(app.getId());
            try {
                reportScheduleRepository.findByAppIdAndActiveTrue(app.getId()).stream()
                        .filter(this::isDue)
                        .forEach(schedule -> {
                            log.info("Running due schedule {} for tenant {}", schedule.getId(), app.getId());
                            reportScheduleService.runNow(schedule.getId());
                        });
            } finally {
                TenantContext.clear();
            }
        }
    }

    boolean isDue(ReportSchedule schedule) {
        return isDue(schedule, LocalDateTime.now());
    }

    boolean isDue(ReportSchedule schedule, LocalDateTime now) {
        LocalTime timeOfDay = parseTime(schedule.getTimeOfDay());
        if (timeOfDay == null || timeOfDay.isAfter(now.toLocalTime())) {
            return false;
        }
        if (schedule.getLastRunAt() == null) {
            return true;
        }
        LocalDate lastRun = schedule.getLastRunAt().atZone(ZoneId.systemDefault()).toLocalDate();
        return switch (schedule.getCadence()) {
            case DAILY -> !lastRun.equals(now.toLocalDate());
            case WEEKLY -> now.getDayOfWeek() == DayOfWeek.MONDAY
                    && lastRun.isBefore(now.toLocalDate().with(DayOfWeek.MONDAY));
            case MONTHLY -> now.getDayOfMonth() == 1
                    && lastRun.getMonthValue() != now.toLocalDate().getMonthValue();
        };
    }

    LocalTime parseTime(String timeOfDay) {
        if (timeOfDay == null || timeOfDay.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(timeOfDay);
        } catch (Exception e) {
            return null;
        }
    }
}