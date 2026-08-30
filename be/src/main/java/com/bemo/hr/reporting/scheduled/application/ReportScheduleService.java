package com.bemo.hr.reporting.scheduled.application;

import com.bemo.hr.reporting.scheduled.domain.ReportSchedule;
import com.bemo.hr.reporting.scheduled.domain.ReportScheduleRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ReportScheduleService {

    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    private final ReportScheduleRepository reportScheduleRepository;
    private final ReportScheduleExecutor scheduleExecutor;

    public ReportSchedule create(ReportScheduleApi.CreateRequest request) {
        String appId = TenantContext.require();
        ReportSchedule schedule = new ReportSchedule(
                appId,
                request.name(),
                ReportSchedule.ReportKind.valueOf(request.reportKind()),
                request.params(),
                ReportSchedule.Channel.valueOf(request.channel()),
                request.recipients(),
                ReportSchedule.Cadence.valueOf(request.cadence()),
                request.timeOfDay()
        );
        return reportScheduleRepository.save(schedule);
    }

    public ReportSchedule update(String id, ReportScheduleApi.UpdateRequest request) {
        ReportSchedule schedule = findByIdOrThrow(id);
        if (request.name() != null) schedule.setName(request.name());
        if (request.reportKind() != null) schedule.setReportKind(ReportSchedule.ReportKind.valueOf(request.reportKind()));
        if (request.params() != null) schedule.setParams(request.params());
        if (request.channel() != null) schedule.setChannel(ReportSchedule.Channel.valueOf(request.channel()));
        if (request.recipients() != null) schedule.setRecipients(request.recipients());
        if (request.cadence() != null) schedule.setCadence(ReportSchedule.Cadence.valueOf(request.cadence()));
        if (request.timeOfDay() != null) schedule.setTimeOfDay(request.timeOfDay());
        if (request.active() != null) schedule.setActive(request.active());
        return reportScheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public List<ReportSchedule> list() {
        return reportScheduleRepository.findByAppIdOrderByCreatedAtDesc(TenantContext.require());
    }

    @Transactional(readOnly = true)
    public ReportSchedule getById(String id) {
        return findByIdOrThrow(id);
    }

    public void delete(String id) {
        ReportSchedule schedule = findByIdOrThrow(id);
        reportScheduleRepository.delete(schedule);
    }

    public ReportSchedule runNow(String id) {
        ReportSchedule schedule = findByIdOrThrow(id);
        try {
            scheduleExecutor.execute(schedule);
        } catch (Exception e) {
            log.error("Schedule {} run failed: {}", id, e.getMessage());
            schedule.markFailed(e.getMessage());
        }
        if (schedule.getConsecutiveFailures() >= MAX_CONSECUTIVE_FAILURES) {
            schedule.deactivate();
            log.warn("Schedule {} auto-disabled after {} consecutive failures", id, MAX_CONSECUTIVE_FAILURES);
        }
        return reportScheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public List<ReportSchedule> findDue() {
        return reportScheduleRepository.findByAppIdAndActiveTrue(TenantContext.require());
    }

    public void recordFailure(ReportSchedule schedule, String error) {
        schedule.markFailed(error);
        if (schedule.getConsecutiveFailures() >= MAX_CONSECUTIVE_FAILURES) {
            schedule.deactivate();
        }
        reportScheduleRepository.save(schedule);
    }

    private ReportSchedule findByIdOrThrow(String id) {
        return reportScheduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report schedule not found", "SCHED_NOT_FOUND"));
    }
}
