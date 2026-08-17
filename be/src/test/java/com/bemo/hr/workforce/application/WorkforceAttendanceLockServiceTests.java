package com.bemo.hr.workforce.application;

import com.bemo.hr.workforce.domain.WorkforceAttendanceLock;
import com.bemo.hr.workforce.infrastructure.WorkforceAttendanceLockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkforceAttendanceLockServiceTests {

    private WorkforceAttendanceLockRepository repository;
    private WorkforceAttendanceLockService service;

    @BeforeEach
    void setUp() {
        repository = mock(WorkforceAttendanceLockRepository.class);
        service = new WorkforceAttendanceLockService(repository);
    }

    @Test
    void locksAttendanceAndCorrectsHoursSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkforceAttendanceLock lock = service.lockAttendance("contractor-1", "2026-08-15", new BigDecimal("160.00"), "admin");
        assertThat(lock).isNotNull();
        assertThat(lock.getTotalHours()).isEqualByComparingTo(new BigDecimal("160.00"));
        assertThat(lock.getStatus()).isEqualTo(WorkforceAttendanceLock.Status.LOCKED);

        when(repository.findById(lock.getId())).thenReturn(Optional.of(lock));

        WorkforceAttendanceLock corrected = service.correctLock(lock.getId(), new BigDecimal("168.00"), "Overtime correction");
        assertThat(corrected.getTotalHours()).isEqualByComparingTo(new BigDecimal("168.00"));
        assertThat(corrected.getStatus()).isEqualTo(WorkforceAttendanceLock.Status.CORRECTED);
    }
}
