package com.bemo.hr.finance.application.close;

import com.bemo.hr.finance.domain.close.PeriodCloseExecutionRecord;
import com.bemo.hr.finance.infrastructure.PeriodCloseExecutionRepository;
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

    @BeforeEach
    void setUp() {
        repository = mock(PeriodCloseExecutionRepository.class);
        mockProvider = mock(ModuleCloseProvider.class);
        when(mockProvider.getModuleName()).thenReturn("GL");
        when(mockProvider.isPeriodCloseReady("2026-08")).thenReturn(true);
        when(mockProvider.getBlockerReason("2026-08")).thenReturn(Optional.empty());

        orchestratorService = new PeriodCloseOrchestratorService(List.of(mockProvider), repository);
    }

    @Test
    void evaluatesReadinessAndExecutesPeriodCloseSuccessfully() {
        PeriodCloseOrchestratorService.PeriodReadinessReport report = orchestratorService.checkReadiness("2026-08");
        assertThat(report.allReady()).isTrue();
        assertThat(report.modules()).hasSize(1);
        assertThat(report.modules().get(0).moduleName()).isEqualTo("GL");

        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<PeriodCloseExecutionRecord> records = orchestratorService.executeClose("2026-08");
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getStatus()).isEqualTo(PeriodCloseExecutionRecord.Status.CLOSED);
        verify(mockProvider).executeClose("2026-08");
    }

    @Test
    void providerFailureFailsClosedAndPreventsExecution() {
        when(mockProvider.isPeriodCloseReady("2026-08")).thenThrow(new IllegalStateException("database unavailable"));

        PeriodCloseOrchestratorService.PeriodReadinessReport report = orchestratorService.checkReadiness("2026-08");

        assertThat(report.allReady()).isFalse();
        assertThat(report.modules().get(0).blockerReason()).contains("Readiness check failed");
        assertThatThrownBy(() -> orchestratorService.executeClose("2026-08"))
                .hasMessageContaining("blocked");
        verify(mockProvider, never()).executeClose(anyString());
    }
}
