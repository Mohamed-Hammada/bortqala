package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.trade.procurement.domain.GrirReconciliationRecord;
import com.bemo.hr.trade.procurement.infrastructure.GrirReconciliationRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GrirReconciliationServiceTests {

    private GrirReconciliationRecordRepository repository;
    private GrirReconciliationService service;

    @BeforeEach
    void setUp() {
        repository = mock(GrirReconciliationRecordRepository.class);
        service = new GrirReconciliationService(repository);
    }

    @Test
    void reconcilesGrirLineAndGeneratesSummaryReportSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GrirReconciliationRecord record = service.reconcileLine("gr-line-1", "inv-line-1", new BigDecimal("1000.00"), new BigDecimal("1000.00"));
        assertThat(record).isNotNull();
        assertThat(record.getStatus()).isEqualTo(GrirReconciliationRecord.Status.BALANCED);
        assertThat(record.getVarianceAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        when(repository.findAll()).thenReturn(List.of(record));

        GrirReconciliationService.GrirSummaryReport summary = service.getSummaryReport();
        assertThat(summary.totalRecords()).isEqualTo(1);
        assertThat(summary.balancedCount()).isEqualTo(1);
        assertThat(summary.varianceCount()).isEqualTo(0);

        when(repository.findById(record.getId())).thenReturn(Optional.of(record));
        service.closeRecord(record.getId());
        assertThat(record.getStatus()).isEqualTo(GrirReconciliationRecord.Status.CLOSED);
    }
}
