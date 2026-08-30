package com.bemo.hr.reporting.scheduled.application;

import com.bemo.hr.reporting.scheduled.domain.ReportSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Orchestrates a single schedule run: render the report bytes, then dispatch them
 * through the matching {@link ReportDeliveryChannel}. Outcome state (success / failure /
 * skipped channel) is applied to the schedule for the caller to persist.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportScheduleExecutor {

    private final ScheduledReportRenderer renderer;
    private final List<ReportDeliveryChannel> deliveryChannels;

    public void execute(ReportSchedule schedule) {
        if (schedule.getChannel() == ReportSchedule.Channel.WHATSAPP) {
            schedule.markSkippedChannel();
            log.info("WhatsApp channel not available for schedule {}, marking SKIPPED", schedule.getId());
            return;
        }

        byte[] content;
        try {
            content = renderer.render(schedule);
        } catch (Exception e) {
            log.error("Schedule {} render failed: {}", schedule.getId(), e.getMessage());
            schedule.markFailed(e.getMessage());
            return;
        }

        ReportDeliveryChannel channel = deliveryChannels.stream()
                .filter(c -> c.channel() == schedule.getChannel())
                .findFirst()
                .orElse(null);
        if (channel == null) {
            schedule.markFailed("No delivery channel configured for " + schedule.getChannel());
            return;
        }

        try {
            boolean delivered = channel.deliver(schedule, content, filenameFor(schedule));
            if (delivered) {
                schedule.markSuccess();
            } else {
                schedule.markFailed("Delivery rejected by " + channel.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Schedule {} delivery failed: {}", schedule.getId(), e.getMessage());
            schedule.markFailed(e.getMessage());
        }
    }

    private String filenameFor(ReportSchedule schedule) {
        return "report-" + schedule.getReportKind().name().toLowerCase(Locale.ROOT)
                + "-" + LocalDate.now() + ".xlsx";
    }
}