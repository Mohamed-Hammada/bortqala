package com.bemo.hr.workforce.application;

import com.bemo.hr.workforce.domain.WorkforceSettlementSnapshot;
import com.bemo.hr.workforce.infrastructure.WorkforceSettlementSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkforceSettlementSnapshotServiceTests {

    private WorkforceSettlementSnapshotRepository repository;
    private WorkforceSettlementSnapshotService service;

    @BeforeEach
    void setUp() {
        repository = mock(WorkforceSettlementSnapshotRepository.class);
        service = new WorkforceSettlementSnapshotService(repository);
    }

    @Test
    void createsFrozenSettlementSnapshotSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkforceSettlementSnapshot snapshot = service.createFrozenSnapshot("contractor-10", "2026-08-15", new BigDecimal("160.00"), new BigDecimal("16000.00"), new BigDecimal("15200.00"));
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getGrossAmount()).isEqualByComparingTo(new BigDecimal("16000.00"));
        assertThat(snapshot.getNetAmount()).isEqualByComparingTo(new BigDecimal("15200.00"));
        assertThat(snapshot.getStatus()).isEqualTo(WorkforceSettlementSnapshot.Status.FROZEN);

        when(repository.findByContractorIdAndPeriodId("contractor-10", "2026-08-15")).thenReturn(Optional.of(snapshot));
        assertThat(service.getSnapshot("contractor-10", "2026-08-15")).isNotNull();
    }
}
