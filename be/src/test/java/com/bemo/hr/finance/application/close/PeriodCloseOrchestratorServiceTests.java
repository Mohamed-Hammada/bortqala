package com.bemo.hr.finance.application.close;

import com.bemo.hr.finance.domain.close.PeriodCloseExecutionRecord;
import com.bemo.hr.finance.infrastructure.PeriodCloseExecutionRepository;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import com.bemo.hr.finance.application.CloseChecklistService;
import com.bemo.hr.finance.application.CloseChecklistSummary;
import com.bemo.hr.finance.domain.FiscalPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PeriodCloseOrchestratorServiceTests {

    private PeriodCloseExecutionRepository repository;
    private ModuleCloseProvider mockProvider;
    private PeriodCloseOrchestratorService orchestratorService;
    private FiscalPeriodRepository fiscalPeriodRepository;
    private CloseChecklistService closeChecklistService;
    private FiscalPeriod period;

    @BeforeEach
    void setUp() {
        repository = mock(PeriodCloseExecutionRepository.class);
        mockProvider = mock(ModuleCloseProvider.class);
        when(mockProvider.getModuleName()).thenReturn("GL");
        when(mockProvider.isPeriodCloseReady("2026-08")).thenReturn(true);
        when(mockProvider.getBlockerReason("2026-08")).thenReturn(Optional.empty());

        fiscalPeriodRepository = mock(FiscalPeriodRepository.class);
        closeChecklistService = mock(CloseChecklistService.class);
        period = new FiscalPeriod(2026, 8, "August", java.time.LocalDate.of(2026, 8, 1),
                java.time.LocalDate.of(2026, 8, 31), FiscalPeriod.Status.OPEN);
        when(fiscalPeriodRepository.findByIdForUpdate("2026-08")).thenReturn(Optional.of(period));
        when(closeChecklistService.computePrecheck("2026-08"))
                .thenReturn(new CloseChecklistSummary(period.getId(), period.getPeriodName(), true, List.of()));
        orchestratorService = new PeriodCloseOrchestratorService(List.of(mockProvider), repository,
                fiscalPeriodRepository, closeChecklistService);
    }

    @Test
    void evaluatesReadinessAndExecutesPeriodCloseSuccessfully() {
        PeriodCloseOrchestratorService.PeriodReadinessReport report = orchestratorService.checkReadiness("2026-08");
        assertThat(report.allReady()).isTrue();
        assertThat(report.modules()).hasSize(1);
        assertThat(report.modules().get(0).moduleName()).isEqualTo("GL");

        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<PeriodCloseExecutionRecord> records = orchestratorService.executeClose("2026-08", "closer", 0L);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getStatus()).isEqualTo(PeriodCloseExecutionRecord.Status.CLOSED);
        verify(mockProvider).executeClose("2026-08");
        assertThat(period.getStatus()).isEqualTo(FiscalPeriod.Status.CLOSED);
        assertThat(period.getClosedBy()).isEqualTo("closer");
        verify(fiscalPeriodRepository).save(period);
    }

    @Test
    void providerFailureFailsClosedAndPreventsExecution() {
        when(mockProvider.isPeriodCloseReady("2026-08")).thenThrow(new IllegalStateException("database unavailable"));

        PeriodCloseOrchestratorService.PeriodReadinessReport report = orchestratorService.checkReadiness("2026-08");

        assertThat(report.allReady()).isFalse();
        assertThat(report.modules().get(0).blockerReason()).contains("Readiness check failed");
        assertThatThrownBy(() -> orchestratorService.executeClose("2026-08", "closer", 0L))
                .hasMessageContaining("blocked");
        verify(mockProvider, never()).executeClose(anyString());
    }
}
