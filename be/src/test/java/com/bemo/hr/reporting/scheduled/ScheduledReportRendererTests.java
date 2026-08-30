package com.bemo.hr.reporting.scheduled;

import com.bemo.hr.reporting.application.DataExportService;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.reporting.scheduled.application.ScheduledReportRenderer;
import com.bemo.hr.reporting.scheduled.domain.ReportSchedule;
import com.bemo.hr.payroll.application.PayrollService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledReportRendererTests {

    @Mock
    private DataExportService dataExportService;

    @Mock
    private PayrollService payrollService;

    private ScheduledReportRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new ScheduledReportRenderer(dataExportService, payrollService, new ObjectMapper());
    }

    private ReportSchedule schedule(ReportSchedule.ReportKind kind, String params) {
        return new ReportSchedule("DEMO", "Test", kind, params, ReportSchedule.Channel.EMAIL,
                "a@b.com", ReportSchedule.Cadence.DAILY, "08:00");
    }

    @Test
    void renderTrends_usesParamsMonths() {
        when(dataExportService.trends(eq(6), any(ExcelExportOptions.class)))
                .thenReturn("PK".getBytes(StandardCharsets.UTF_8));
        ReportSchedule schedule = schedule(ReportSchedule.ReportKind.TRENDS, "{\"months\":6}");

        byte[] bytes = renderer.render(schedule);

        assertArrayEquals("PK".getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test
    void renderTrends_missingParams_defaultsToTwelveMonths() {
        when(dataExportService.trends(eq(12), any(ExcelExportOptions.class)))
                .thenReturn(new byte[]{1});
        ReportSchedule schedule = schedule(ReportSchedule.ReportKind.TRENDS, "{}");

        assertArrayEquals(new byte[]{1}, renderer.render(schedule));
    }

    @Test
    void renderAttendance_delegatesToClockInHistogram() {
        when(dataExportService.clockInHistogram(eq(3), eq("CAT-1"), any(ExcelExportOptions.class)))
                .thenReturn(new byte[]{2});
        ReportSchedule schedule = schedule(ReportSchedule.ReportKind.ATTENDANCE, "{\"months\":3,\"categoryId\":\"CAT-1\"}");

        assertArrayEquals(new byte[]{2}, renderer.render(schedule));
    }

    @Test
    void renderPayroll_delegatesToPayrollExport() {
        when(payrollService.export(eq(2026), eq(8), eq((String) null), any(ExcelExportOptions.class)))
                .thenReturn(new byte[]{3});
        ReportSchedule schedule = schedule(ReportSchedule.ReportKind.PAYROLL, "{\"year\":2026,\"month\":8}");

        assertArrayEquals(new byte[]{3}, renderer.render(schedule));
    }

    @Test
    void renderUnsupportedKind_throwsBusinessRule() {
        ReportSchedule schedule = schedule(ReportSchedule.ReportKind.CASHFLOW, "{}");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> renderer.render(schedule));
        assertEquals("SCHED_RENDER_UNSUPPORTED", ex.getCode());
    }
}