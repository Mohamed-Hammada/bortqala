package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollComponent;
import com.bemo.hr.payroll.infrastructure.PayrollComponentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PayrollComponentEvaluatorServiceTests {

    private PayrollComponentRepository repository;
    private PayrollComponentEvaluatorService service;

    @BeforeEach
    void setUp() {
        repository = mock(PayrollComponentRepository.class);
        service = new PayrollComponentEvaluatorService(repository);
    }

    @Test
    void createsAndEvaluatesComponentSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PayrollComponent comp = service.createComponent("HOUSING", "Housing Allowance", PayrollComponent.Type.EARNING, "BASE * 20%");
        assertThat(comp).isNotNull();
        assertThat(comp.getCode()).isEqualTo("HOUSING");

        when(repository.findById(comp.getId())).thenReturn(Optional.of(comp));

        PayrollComponentEvaluatorService.EvaluationResult result = service.evaluateComponent(comp.getId(), new BigDecimal("10000.00"), new BigDecimal("20.00"));
        assertThat(result.evaluatedAmount()).isEqualByComparingTo(new BigDecimal("2000.00"));
    }
}
