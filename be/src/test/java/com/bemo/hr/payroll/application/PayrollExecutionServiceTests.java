package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollRunHeader;
import com.bemo.hr.payroll.domain.PayrollInputSnapshot;
import com.bemo.hr.payroll.domain.PayrollRunLine;
import com.bemo.hr.payroll.infrastructure.PayrollRunHeaderRepository;
import com.bemo.hr.payroll.infrastructure.PayrollRunLineRepository;
import com.bemo.hr.payroll.infrastructure.PayrollInputSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PayrollExecutionServiceTests {

    private PayrollRunHeaderRepository runHeaderRepository;
    private PayrollRunLineRepository runLineRepository;
    private PayrollExecutionService executionService;
    private PayrollInputSnapshotRepository snapshotRepository;

    @BeforeEach
    void setUp() {
        runHeaderRepository = mock(PayrollRunHeaderRepository.class);
        runLineRepository = mock(PayrollRunLineRepository.class);
        snapshotRepository = mock(PayrollInputSnapshotRepository.class);
        executionService = new PayrollExecutionService(runHeaderRepository, runLineRepository, snapshotRepository);
    }

    @Test
    void createsCalculatesApprovesAndPostsPayrollRunSuccessfully() {
        when(runHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PayrollRunHeader run = executionService.createRun("RUN-2026-03", "period-1", LocalDate.of(2026, 3, 31));
        assertThat(run).isNotNull();
        assertThat(run.getStatus()).isEqualTo(PayrollRunHeader.Status.DRAFT);

        PayrollInputSnapshot snapshot = new PayrollInputSnapshot(run.getId(), "emp-1", "period-1",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), new BigDecimal("5000"),
                9600, 0, 0, 0, "policy", 1, new BigDecimal("240"), new BigDecimal("1.5"),
                new BigDecimal("500"), new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("6000"), new BigDecimal("5500"), "maker");
        when(runLineRepository.findByRunId(run.getId())).thenReturn(List.of());
        when(snapshotRepository.findByPayrollRunId(run.getId())).thenReturn(List.of(snapshot));
        when(runHeaderRepository.findById(run.getId())).thenReturn(Optional.of(run));
        when(runHeaderRepository.findByIdForUpdate(run.getId())).thenReturn(Optional.of(run));

        executionService.calculateRun(run.getId());
        assertThat(run.getStatus()).isEqualTo(PayrollRunHeader.Status.CALCULATED);
        assertThat(run.getTotalGross()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(run.getTotalNet()).isEqualByComparingTo(new BigDecimal("5500.00"));

        executionService.approveRun(run.getId());
        assertThat(run.getStatus()).isEqualTo(PayrollRunHeader.Status.APPROVED);

        executionService.postRun(run.getId());
        assertThat(run.getStatus()).isEqualTo(PayrollRunHeader.Status.POSTED);
    }

    @Test
    void rejectsManualRunLinesAndCalculationWithoutSnapshots() {
        PayrollRunHeader run = new PayrollRunHeader("RUN-2", "period-2", LocalDate.now());
        when(runHeaderRepository.findByIdForUpdate(run.getId())).thenReturn(Optional.of(run));
        when(snapshotRepository.findByPayrollRunId(run.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> executionService.addRunLine(run.getId(), "emp-1", BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ZERO)).isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class);
        assertThatThrownBy(() -> executionService.calculateRun(run.getId()))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class);
    }
}
