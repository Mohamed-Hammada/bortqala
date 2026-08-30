package com.bemo.hr.reporting.scheduled.application;

import com.bemo.hr.reporting.scheduled.domain.ReportSchedule;

/**
 * Outbound delivery for rendered scheduled reports. Implementations are discovered
 * as Spring beans and matched to a schedule by {@link #channel()}.
 */
public interface ReportDeliveryChannel {

    ReportSchedule.Channel channel();

    /**
     * @return true when the recipient delivery was accepted / dispatched successfully
     */
    boolean deliver(ReportSchedule schedule, byte[] content, String filename);
}