package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollPaymentBatch;
import com.bemo.hr.payroll.infrastructure.PayrollPaymentBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayrollPaymentBatchServiceTests {

    private PayrollPaymentBatchRepository repository;
    private PayrollPaymentBatchService service;

    @BeforeEach
    void setUp() {
        repository = mock(PayrollPaymentBatchRepository.class);
        service = new PayrollPaymentBatchService(repository);
    }

    @Test
    void createsAndProcessesPayrollPaymentBatchSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PayrollPaymentBatch batch = service.createBatch("period-2026-08", new BigDecimal("50000.00"), 10);
        assertThat(batch).isNotNull();
        assertThat(batch.getTotalAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(batch.getEmployeeCount()).isEqualTo(10);
        assertThat(batch.getStatus()).isEqualTo(PayrollPaymentBatch.Status.DRAFT);

        when(repository.findById(batch.getId())).thenReturn(Optional.of(batch));

        PayrollPaymentBatch processed = service.processBatch(batch.getId());
        assertThat(processed.getStatus()).isEqualTo(PayrollPaymentBatch.Status.PROCESSED);
    }
}
