package com.bemo.hr.shared.job.infrastructure;

import com.bemo.hr.shared.job.domain.ScheduledJobExecutionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledJobExecutionRepository extends JpaRepository<ScheduledJobExecutionRecord, String> {
    Optional<ScheduledJobExecutionRecord> findByJobNameAndExecutionKey(String jobName, String executionKey);

    List<ScheduledJobExecutionRecord> findByJobName(String jobName);
}
