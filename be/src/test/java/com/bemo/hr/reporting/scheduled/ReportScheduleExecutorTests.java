package com.bemo.hr.reporting.scheduled;

import com.bemo.hr.reporting.scheduled.application.ReportDeliveryChannel;
import com.bemo.hr.reporting.scheduled.application.ReportScheduleExecutor;
import com.bemo.hr.reporting.scheduled.application.ScheduledReportRenderer;
import com.bemo.hr.reporting.scheduled.domain.ReportSchedule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportScheduleExecutorTests {

    @Mock
    private ScheduledReportRenderer renderer;

    @Mock
    private ReportDeliveryChannel emailChannel;

    @Mock
    private ReportDeliveryChannel whatsappChannel;

    private ReportScheduleExecutor executor() {
        return new ReportScheduleExecutor(renderer, List.of(emailChannel, whatsappChannel));
    }

    private ReportSchedule schedule(ReportSchedule.Channel channel, ReportSchedule.ReportKind kind) {
        return new ReportSchedule("DEMO", "Test", kind, "{}", channel, "a@b.com",
                ReportSchedule.Cadence.DAILY, "08:00");
    }

    @Test
    void execute_whatsappChannel_marksSkipped() {
        ReportSchedule schedule = schedule(ReportSchedule.Channel.WHATSAPP, ReportSchedule.ReportKind.TRENDS);

        executor().execute(schedule);

        assertEquals("SKIPPED_CHANNEL", schedule.getLastStatus());
        verify(renderer, never()).render(any());
        verify(emailChannel, never()).deliver(any(), any(), any());
    }

    @Test
    void execute_renderAndDeliver_successMarked() {
        when(emailChannel.channel()).thenReturn(ReportSchedule.Channel.EMAIL);
        when(renderer.render(any(ReportSchedule.class))).thenReturn(new byte[]{1, 2, 3, 'P', 'K'});
        when(emailChannel.deliver(any(), any(), any())).thenReturn(true);
        ReportSchedule schedule = schedule(ReportSchedule.Channel.EMAIL, ReportSchedule.ReportKind.TRENDS);

        executor().execute(schedule);

        assertEquals("SUCCESS", schedule.getLastStatus());
        verify(emailChannel).deliver(eq(schedule), any(byte[].class), eq("report-trends-" + java.time.LocalDate.now() + ".xlsx"));
    }

    @Test
    void execute_renderFailure_marksFailed() {
        when(renderer.render(any(ReportSchedule.class)))
                .thenThrow(new RuntimeException("SCHED_RENDER_UNSUPPORTED: CASHFLOW"));
        ReportSchedule schedule = schedule(ReportSchedule.Channel.EMAIL, ReportSchedule.ReportKind.CASHFLOW);

        executor().execute(schedule);

        assertEquals("FAILED", schedule.getLastStatus());
        assertTrue(schedule.getLastError().contains("SCHED_RENDER_UNSUPPORTED"));
        verify(emailChannel, never()).deliver(any(), any(), any());
    }

    @Test
    void execute_noMatchingChannel_marksFailed() {
        when(renderer.render(any(ReportSchedule.class))).thenReturn(new byte[]{1});
        ReportSchedule schedule = schedule(ReportSchedule.Channel.EMAIL, ReportSchedule.ReportKind.TRENDS);
        var executor = new ReportScheduleExecutor(renderer, List.of());

        executor.execute(schedule);

        assertEquals("FAILED", schedule.getLastStatus());
        assertTrue(schedule.getLastError().contains("No delivery channel configured"));
    }

    @Test
    void execute_deliveryRejected_marksFailed() {
        when(emailChannel.channel()).thenReturn(ReportSchedule.Channel.EMAIL);
        when(renderer.render(any(ReportSchedule.class))).thenReturn(new byte[]{1});
        when(emailChannel.deliver(any(), any(), any())).thenReturn(false);
        ReportSchedule schedule = schedule(ReportSchedule.Channel.EMAIL, ReportSchedule.ReportKind.TRENDS);

        executor().execute(schedule);

        assertEquals("FAILED", schedule.getLastStatus());
        assertTrue(schedule.getLastError().contains("rejected"));
    }
}