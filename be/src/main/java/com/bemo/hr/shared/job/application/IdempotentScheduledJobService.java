package com.bemo.hr.shared.job.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.job.domain.ScheduledJobExecutionRecord;
import com.bemo.hr.shared.job.infrastructure.ScheduledJobExecutionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class IdempotentScheduledJobService {

    private final ScheduledJobExecutionRepository repository;

    public IdempotentScheduledJobService(ScheduledJobExecutionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ScheduledJobExecutionRecord executeJobIdempotently(String jobName, String executionKey, Runnable task) {
        Optional<ScheduledJobExecutionRecord> existing = repository.findByJobNameAndExecutionKey(jobName, executionKey);
        if (existing.isPresent()) {
            ScheduledJobExecutionRecord record = existing.get();
            if (record.getStatus() == ScheduledJobExecutionRecord.Status.COMPLETED) {
                return record; // Idempotent skip
            }
            if (record.getStatus() == ScheduledJobExecutionRecord.Status.RUNNING) {
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
            throw e;
        }

        return repository.save(record);
    }

    @Transactional(readOnly = true)
    public List<ScheduledJobExecutionRecord> getExecutions(String jobName) {
        return repository.findByJobName(jobName);
    }
}
