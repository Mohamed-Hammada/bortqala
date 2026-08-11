package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.ProductionVarianceClose;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionVarianceCloseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ManufacturingVarianceCloseServiceTests {

    private ProductionVarianceCloseRepository repository;
    private ManufacturingVarianceCloseService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductionVarianceCloseRepository.class);
        service = new ManufacturingVarianceCloseService(repository);
    }

    @Test
    void calculatesAndClosesVarianceSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductionVarianceClose close = service.calculateAndCloseVariance("wo-200", new BigDecimal("10000.00"), new BigDecimal("10500.00"));
        assertThat(close).isNotNull();
        assertThat(close.getStandardCost()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(close.getActualCost()).isEqualByComparingTo(new BigDecimal("10500.00"));
        assertThat(close.getVarianceCost()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(close.getStatus()).isEqualTo(ProductionVarianceClose.Status.CLOSED);

        when(repository.findByWorkOrderId("wo-200")).thenReturn(Optional.of(close));
        ProductionVarianceClose fetched = service.getVarianceClose("wo-200");
        assertThat(fetched).isNotNull();
    }
}
