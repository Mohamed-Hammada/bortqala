package com.bemo.hr.shared.job.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.job.domain.ScheduledJobExecutionRecord;
import com.bemo.hr.shared.job.infrastructure.ScheduledJobExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class IdempotentScheduledJobService {

    private final ScheduledJobExecutionRepository repository;

    public IdempotentScheduledJobService(ScheduledJobExecutionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ScheduledJobExecutionRecord executeJobIdempotently(String jobName, String executionKey, Runnable task) {
        log.debug("executeJobIdempotently called with jobName={}, executionKey={}", jobName, executionKey);
        Optional<ScheduledJobExecutionRecord> existing = repository.findByJobNameAndExecutionKey(jobName, executionKey);
        if (existing.isPresent()) {
            ScheduledJobExecutionRecord record = existing.get();
            if (record.getStatus() == ScheduledJobExecutionRecord.Status.COMPLETED) {
                log.debug("Job {} execution={} already completed, skipping", jobName, executionKey);
                return record;
            }
            if (record.getStatus() == ScheduledJobExecutionRecord.Status.RUNNING) {
                log.warn("Job {} execution={} is already running", jobName, executionKey);
                throw new BusinessRuleException("Job is already running for execution key: " + executionKey, "JOB_ALREADY_RUNNING", HttpStatus.CONFLICT);
            }
        }

        ScheduledJobExecutionRecord record = existing.orElseGet(() -> new ScheduledJobExecutionRecord(jobName, executionKey));
        repository.save(record);

        try {
            task.run();
            record.markCompleted();
        } catch (Exception e) {
            record.markFailed(e.getMessage());
            repository.save(record);
            log.error("Job {} execution={} failed", jobName, executionKey, e);
            throw e;
        }

        ScheduledJobExecutionRecord saved = repository.save(record);
        log.info("Job {} execution={} completed successfully", jobName, executionKey);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ScheduledJobExecutionRecord> getExecutions(String jobName) {
        return repository.findByJobName(jobName);
    }
}
