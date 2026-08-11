package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollRunHeader;
import com.bemo.hr.payroll.domain.PayrollRunLine;
import com.bemo.hr.payroll.infrastructure.PayrollRunHeaderRepository;
import com.bemo.hr.payroll.infrastructure.PayrollRunLineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PayrollExecutionServiceTests {

    private PayrollRunHeaderRepository runHeaderRepository;
    private PayrollRunLineRepository runLineRepository;
    private PayrollExecutionService executionService;

    @BeforeEach
    void setUp() {
        runHeaderRepository = mock(PayrollRunHeaderRepository.class);
        runLineRepository = mock(PayrollRunLineRepository.class);
        executionService = new PayrollExecutionService(runHeaderRepository, runLineRepository);
    }

    @Test
    void createsCalculatesApprovesAndPostsPayrollRunSuccessfully() {
        when(runHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PayrollRunHeader run = executionService.createRun("RUN-2026-03", "period-1", LocalDate.of(2026, 3, 31));
        assertThat(run).isNotNull();
        assertThat(run.getStatus()).isEqualTo(PayrollRunHeader.Status.DRAFT);

        PayrollRunLine l1 = new PayrollRunLine(run.getId(), "emp-1", new BigDecimal("5000.00"), new BigDecimal("1000.00"), new BigDecimal("500.00"));
        when(runLineRepository.findByRunId(run.getId())).thenReturn(List.of(l1));
        when(runHeaderRepository.findById(run.getId())).thenReturn(Optional.of(run));

        executionService.calculateRun(run.getId());
        assertThat(run.getStatus()).isEqualTo(PayrollRunHeader.Status.CALCULATED);
        assertThat(run.getTotalGross()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(run.getTotalNet()).isEqualByComparingTo(new BigDecimal("5500.00"));

        executionService.approveRun(run.getId());
        assertThat(run.getStatus()).isEqualTo(PayrollRunHeader.Status.APPROVED);

        executionService.postRun(run.getId());
        assertThat(run.getStatus()).isEqualTo(PayrollRunHeader.Status.POSTED);
    }
}
