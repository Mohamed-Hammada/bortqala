package com.bemo.hr.finance.application.close;

import com.bemo.hr.finance.domain.close.PeriodCloseExecutionRecord;
import com.bemo.hr.finance.infrastructure.PeriodCloseExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PeriodCloseWorkbenchServiceTests {

    private PeriodCloseExecutionRepository executionRepository;
    private ModuleCloseProvider mockProvider;
    private PeriodCloseWorkbenchService workbenchService;

    @BeforeEach
    void setUp() {
        executionRepository = mock(PeriodCloseExecutionRepository.class);
        mockProvider = mock(ModuleCloseProvider.class);
        when(mockProvider.getModuleName()).thenReturn("WORKFORCE");
        when(mockProvider.isPeriodCloseReady("2026-08")).thenReturn(true);
        when(mockProvider.getBlockerReason("2026-08")).thenReturn(Optional.empty());

        workbenchService = new PeriodCloseWorkbenchService(List.of(mockProvider), executionRepository);
    }

    @Test
    void aggregatesWorkbenchSummaryAcrossModulesSuccessfully() {
        PeriodCloseExecutionRecord record = new PeriodCloseExecutionRecord("2026-08", "WORKFORCE", PeriodCloseExecutionRecord.Status.CLOSED, null);

        when(executionRepository.findByPeriodId("2026-08")).thenReturn(List.of(record));

        PeriodCloseWorkbenchService.WorkbenchSummary summary = workbenchService.getWorkbenchSummary("2026-08");

        assertThat(summary).isNotNull();
        assertThat(summary.totalModules()).isEqualTo(1);
        assertThat(summary.readyModules()).isEqualTo(1);
        assertThat(summary.executedModules()).isEqualTo(1);
        assertThat(summary.moduleStatuses()).hasSize(1);
        assertThat(summary.moduleStatuses().get(0).moduleName()).isEqualTo("WORKFORCE");
        assertThat(summary.moduleStatuses().get(0).isExecuted()).isTrue();
    }
}
