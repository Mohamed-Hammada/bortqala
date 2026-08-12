package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollRetroAdjustment;
import com.bemo.hr.payroll.infrastructure.PayrollRetroAdjustmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PayrollRetroAdjustmentServiceTests {

    private PayrollRetroAdjustmentRepository repository;
    private PayrollRetroAdjustmentService service;

    @BeforeEach
    void setUp() {
        repository = mock(PayrollRetroAdjustmentRepository.class);
        service = new PayrollRetroAdjustmentService(repository);
    }

    @Test
    void createsApprovesAndProcessesPayrollRetroAdjustmentSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PayrollRetroAdjustment adj = service.createAdjustment("emp-99", "2026-08", "BACKPAY", new BigDecimal("1500.00"), "Missed overtime adjustment");
        assertThat(adj).isNotNull();
        assertThat(adj.getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(adj.getStatus()).isEqualTo(PayrollRetroAdjustment.Status.PENDING);

        when(repository.findById(adj.getId())).thenReturn(Optional.of(adj));

        PayrollRetroAdjustment approved = service.approveAdjustment(adj.getId());
        assertThat(approved.getStatus()).isEqualTo(PayrollRetroAdjustment.Status.APPROVED);

        PayrollRetroAdjustment processed = service.processAdjustment(adj.getId());
        assertThat(processed.getStatus()).isEqualTo(PayrollRetroAdjustment.Status.PROCESSED);
    }
}
