package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollGlPosting;
import com.bemo.hr.payroll.infrastructure.PayrollGlPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PayrollGlPostingServiceTests {

    private PayrollGlPostingRepository repository;
    private PayrollGlPostingService service;

    @BeforeEach
    void setUp() {
        repository = mock(PayrollGlPostingRepository.class);
        service = new PayrollGlPostingService(repository);
    }

    @Test
    void postsPayrollRunToGlSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PayrollGlPosting posting = service.postPayrollToGl("period-2026-08", "jrnl-900", new BigDecimal("120000.00"), new BigDecimal("105000.00"));
        assertThat(posting).isNotNull();
        assertThat(posting.getGrossAmount()).isEqualByComparingTo(new BigDecimal("120000.00"));
        assertThat(posting.getNetAmount()).isEqualByComparingTo(new BigDecimal("105000.00"));
        assertThat(posting.getStatus()).isEqualTo(PayrollGlPosting.Status.POSTED);

        when(repository.findByPayrollPeriodId("period-2026-08")).thenReturn(Optional.of(posting));
        assertThat(service.getGlPosting("period-2026-08")).isNotNull();
    }
}
