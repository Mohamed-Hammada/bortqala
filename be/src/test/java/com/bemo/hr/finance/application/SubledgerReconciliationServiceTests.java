package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;
import com.bemo.hr.finance.infrastructure.SubledgerReconciliationReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubledgerReconciliationServiceTests {

    private SubledgerReconciliationReportRepository repository;
    private SubledgerReconciliationService service;

    @BeforeEach
    void setUp() {
        repository = mock(SubledgerReconciliationReportRepository.class);
        service = new SubledgerReconciliationService(repository);
    }

    @Test
    void generatesSubledgerReconciliationReportSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubledgerReconciliationReport report = service.generateReport("2026-08", SubledgerReconciliationReport.SubledgerType.AP, new BigDecimal("50000.00"), new BigDecimal("50000.00"));
        assertThat(report).isNotNull();
        assertThat(report.getVarianceAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.getSubledgerType()).isEqualTo(SubledgerReconciliationReport.SubledgerType.AP);

        when(repository.findByPeriodId("2026-08")).thenReturn(List.of(report));
        List<SubledgerReconciliationReport> list = service.getReportsByPeriod("2026-08");
        assertThat(list).hasSize(1);
    }
}
