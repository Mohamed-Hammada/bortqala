package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;
import com.bemo.hr.finance.infrastructure.SubledgerReconciliationReportRepository;
import com.bemo.hr.finance.application.close.SubledgerReconciliationProvider;
import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubledgerReconciliationServiceTests {

    private SubledgerReconciliationReportRepository repository;
    private SubledgerReconciliationService service;

    @BeforeEach
    void setUp() {
        repository = mock(SubledgerReconciliationReportRepository.class);
    }

    @Test
    void generatesReportOnlyFromServerProviderBalances() {
        FiscalPeriodRepository periods = mock(FiscalPeriodRepository.class);
        FiscalPeriod period = new FiscalPeriod(2026, 8, "August", LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), FiscalPeriod.Status.OPEN);
        when(periods.findById("2026-08")).thenReturn(Optional.of(period));
        SubledgerReconciliationProvider provider = mock(SubledgerReconciliationProvider.class);
        when(provider.type()).thenReturn(SubledgerReconciliationReport.SubledgerType.AP);
        when(provider.calculate("2026-08", LocalDate.of(2026, 8, 31))).thenReturn(
                new SubledgerReconciliationProvider.ReconciliationCalculation(
                        SubledgerReconciliationReport.SubledgerType.AP, new BigDecimal("50000.00"),
                        new BigDecimal("50000.00"), BigDecimal.ZERO, true, List.of()));
        service = new SubledgerReconciliationService(repository, List.of(provider), periods, new ObjectMapper());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubledgerReconciliationReport report = service.generateReport("2026-08", SubledgerReconciliationReport.SubledgerType.AP);
        assertThat(report).isNotNull();
        assertThat(report.getVarianceAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.getSubledgerType()).isEqualTo(SubledgerReconciliationReport.SubledgerType.AP);

        when(repository.findByPeriodId("2026-08")).thenReturn(List.of(report));
        List<SubledgerReconciliationReport> list = service.getReportsByPeriod("2026-08");
        assertThat(list).hasSize(1);
    }

    @Test
    void persistsClosedPeriodAsOfDateAndSourceDifferences() {
        FiscalPeriodRepository periods = mock(FiscalPeriodRepository.class);
        FiscalPeriod period = new FiscalPeriod(2026, 8, "August", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), FiscalPeriod.Status.CLOSED);
        when(periods.findById("period-8")).thenReturn(Optional.of(period));
        SubledgerReconciliationProvider provider = mock(SubledgerReconciliationProvider.class);
        when(provider.type()).thenReturn(SubledgerReconciliationReport.SubledgerType.AR);
        when(provider.calculate("period-8", LocalDate.of(2026, 8, 31))).thenReturn(new SubledgerReconciliationProvider.ReconciliationCalculation(
                SubledgerReconciliationReport.SubledgerType.AR, new BigDecimal("90"), new BigDecimal("100"),
                new BigDecimal("-10"), false, List.of(new SubledgerReconciliationProvider.SourceDifference(
                "inv-1", "INV-1", new BigDecimal("100"), new BigDecimal("90")))));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new SubledgerReconciliationService(repository, List.of(provider), periods, new ObjectMapper());

        SubledgerReconciliationReport report = service.generateReport("period-8", SubledgerReconciliationReport.SubledgerType.AR);

        assertThat(report.getAsOfDate()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(report.getVarianceAmount()).isEqualByComparingTo("-10");
        assertThat(report.getDifferenceDetails()).contains("INV-1", "100", "90");
    }

    @Test
    void rejectsMissingProviderInsteadOfPersistingZeroBalances() {
        FiscalPeriodRepository periods = mock(FiscalPeriodRepository.class);
        FiscalPeriod period = new FiscalPeriod(2026, 8, "August", LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), FiscalPeriod.Status.OPEN);
        when(periods.findById("period-8")).thenReturn(Optional.of(period));
        service = new SubledgerReconciliationService(repository, List.of(), periods, new ObjectMapper());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.generateReport(
                        "period-8", SubledgerReconciliationReport.SubledgerType.PAYROLL))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class)
                .extracting("code").isEqualTo("FIN_RECONCILIATION_PROVIDER_REQUIRED");
        verify(repository, never()).save(any());
    }
}
