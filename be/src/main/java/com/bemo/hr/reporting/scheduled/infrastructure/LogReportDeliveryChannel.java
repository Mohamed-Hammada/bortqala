package com.bemo.hr.reporting.scheduled.infrastructure;

import com.bemo.hr.reporting.scheduled.application.ReportDeliveryChannel;
import com.bemo.hr.reporting.scheduled.domain.ReportSchedule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogReportDeliveryChannel implements ReportDeliveryChannel {

    @Override
    public ReportSchedule.Channel channel() {
        return ReportSchedule.Channel.EMAIL;
    }

    @Override
    public boolean deliver(ReportSchedule schedule, byte[] content, String filename) {
        log.info("Dispatching scheduled report '{}' ({}) via EMAIL to [{}] - {} ({} bytes)",
                schedule.getName(), schedule.getId(), schedule.getRecipients(), filename, content.length);
        return true;
    }
}