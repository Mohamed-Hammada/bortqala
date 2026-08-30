package com.bemo.hr.reporting.scheduled.application;

import com.bemo.hr.reporting.scheduled.domain.ReportSchedule;
import com.bemo.hr.reporting.scheduled.domain.ReportScheduleRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportScheduleSchedulerTests {

    @Mock
    private TenantApplicationRepository tenantApplicationRepository;

    @Mock
    private ReportScheduleRepository reportScheduleRepository;

    @Mock
    private ReportScheduleService reportScheduleService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private ReportScheduleScheduler scheduler() {
        return new ReportScheduleScheduler(
                tenantApplicationRepository, reportScheduleRepository, reportScheduleService);
    }

    private ReportSchedule schedule(ReportSchedule.Cadence cadence, String timeOfDay) {
        return new ReportSchedule("DEMO", "Test", ReportSchedule.ReportKind.TRENDS, "{}",
                ReportSchedule.Channel.EMAIL, "a@b.com", cadence, timeOfDay);
    }

    private static void setLastRunAt(ReportSchedule schedule, Instant lastRunAt) {
        try {
            var field = ReportSchedule.class.getDeclaredField("lastRunAt");
            field.setAccessible(true);
            field.set(schedule, lastRunAt);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Instant instantOn(LocalDateTime now) {
        return now.atZone(ZoneId.systemDefault()).toInstant();
    }

    @Test
    void neverRun_beforeTime_notDue() {
        var s = schedule(ReportSchedule.Cadence.DAILY, "23:59");

        assertFalse(scheduler().isDue(s, LocalDateTime.of(2026, 8, 25, 9, 0)));
    }

    @Test
    void neverRun_afterTime_due() {
        var s = schedule(ReportSchedule.Cadence.DAILY, "08:00");

        assertTrue(scheduler().isDue(s, LocalDateTime.of(2026, 8, 25, 9, 0)));
    }

    @Test
    void daily_alreadyRunToday_notDue() {
        var s = schedule(ReportSchedule.Cadence.DAILY, "00:00");
        setLastRunAt(s, instantOn(LocalDateTime.of(2026, 8, 25, 6, 0)));

        assertFalse(scheduler().isDue(s, LocalDateTime.of(2026, 8, 25, 9, 0)));
    }

    @Test
    void daily_lastRunYesterday_due() {
        var stale = schedule(ReportSchedule.Cadence.DAILY, "00:00");
        setLastRunAt(stale, instantOn(LocalDateTime.of(2026, 8, 24, 23, 59)));

        assertTrue(scheduler().isDue(stale, LocalDateTime.of(2026, 8, 25, 9, 0)));

        var fresh = schedule(ReportSchedule.Cadence.DAILY, "00:00");
        setLastRunAt(fresh, instantOn(LocalDateTime.of(2026, 8, 25, 8, 0)));
        assertFalse(scheduler().isDue(fresh, LocalDateTime.of(2026, 8, 25, 9, 0)));
    }

    @Test
    void weekly_dueOnMondayAfterLastWeek() {
        var s = schedule(ReportSchedule.Cadence.WEEKLY, "00:00");
        setLastRunAt(s, instantOn(LocalDateTime.of(2026, 8, 17, 12, 0)));

        var mondayNow = LocalDateTime.of(2026, 8, 24, 9, 0);
        assertTrue(scheduler().isDue(s, mondayNow));

        var nonMondayNow = LocalDateTime.of(2026, 8, 25, 9, 0);
        assertFalse(scheduler().isDue(s, nonMondayNow));
    }

    @Test
    void monthly_notFirstDay_notDue() {
        var s = schedule(ReportSchedule.Cadence.MONTHLY, "00:00");
        setLastRunAt(s, instantOn(LocalDateTime.of(2026, 8, 10, 12, 0)));

        assertFalse(scheduler().isDue(s, LocalDateTime.of(2026, 8, 15, 9, 0)));
    }

    @Test
    void monthly_firstDayNeverRun_due() {
        var s = schedule(ReportSchedule.Cadence.MONTHLY, "00:00");

        assertTrue(scheduler().isDue(s, LocalDateTime.of(2026, 8, 1, 9, 0)));
    }

    @Test
    void scan_runsDueSchedulesPerTenant_andClearsContext() {
        var tenant = new TenantApplication("T1", "Tenant One");
        var due = new ReportSchedule(tenant.getId(), "Due", ReportSchedule.ReportKind.TRENDS, "{}",
                ReportSchedule.Channel.EMAIL, "a@b.com", ReportSchedule.Cadence.DAILY, "00:00");
        var notDue = new ReportSchedule(tenant.getId(), "Later", ReportSchedule.ReportKind.TRENDS, "{}",
                ReportSchedule.Channel.EMAIL, "a@b.com", ReportSchedule.Cadence.DAILY, "23:59");
        when(tenantApplicationRepository.findAll()).thenReturn(List.of(tenant));
        when(reportScheduleRepository.findByAppIdAndActiveTrue(tenant.getId()))
                .thenReturn(List.of(due, notDue));

        scheduler().runDueSchedules();

        verify(reportScheduleService).runNow(due.getId());
        verify(reportScheduleService, never()).runNow(notDue.getId());
        assertNull(TenantContext.current());
    }

    @Test
    void parseTime_invalid_returnsNull() {
        var scheduler = scheduler();
        assertNull(scheduler.parseTime("abc"));
        assertNull(scheduler.parseTime(null));
        assertNotNull(scheduler.parseTime("09:30"));
    }

    @Test
    void dayBoundary_midnightToMidnightUsesSameDay() {
        var s = schedule(ReportSchedule.Cadence.DAILY, "00:00");
        setLastRunAt(s, instantOn(LocalDateTime.of(2026, 8, 25, 23, 58)));

        var justBeforeMidnight = LocalDateTime.of(2026, 8, 25, 23, 59);
        assertFalse(scheduler().isDue(s, justBeforeMidnight));
    }
}