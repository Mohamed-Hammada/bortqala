package com.bemo.hr.shared.job.application;

import com.bemo.hr.shared.job.domain.ScheduledJobExecutionRecord;
import com.bemo.hr.shared.job.infrastructure.ScheduledJobExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdempotentScheduledJobServiceTests {

    private ScheduledJobExecutionRepository repository;
    private IdempotentScheduledJobService service;

    @BeforeEach
    void setUp() {
        repository = mock(ScheduledJobExecutionRepository.class);
        service = new IdempotentScheduledJobService(repository);
    }

    @Test
    void executesJobIdempotentlyAndSkipsDuplicateCompletedKey() {
        when(repository.findByJobNameAndExecutionKey("DEPRECIATION", "2026-08-31")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AtomicInteger count = new AtomicInteger(0);

        ScheduledJobExecutionRecord record = service.executeJobIdempotently("DEPRECIATION", "2026-08-31", count::incrementAndGet);
        assertThat(record).isNotNull();
        assertThat(record.getStatus()).isEqualTo(ScheduledJobExecutionRecord.Status.COMPLETED);
        assertThat(count.get()).isEqualTo(1);

        // Second run with COMPLETED status
        when(repository.findByJobNameAndExecutionKey("DEPRECIATION", "2026-08-31")).thenReturn(Optional.of(record));

        ScheduledJobExecutionRecord secondRecord = service.executeJobIdempotently("DEPRECIATION", "2026-08-31", count::incrementAndGet);
        assertThat(secondRecord.getStatus()).isEqualTo(ScheduledJobExecutionRecord.Status.COMPLETED);
        assertThat(count.get()).isEqualTo(1); // Task was NOT executed second time
    }
}
