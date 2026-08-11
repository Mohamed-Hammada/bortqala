package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.ExchangeRateRecord;
import com.bemo.hr.finance.infrastructure.ExchangeRateRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ForeignExchangeEngineServiceTests {

    private ExchangeRateRecordRepository repository;
    private ForeignExchangeEngineService service;

    @BeforeEach
    void setUp() {
        repository = mock(ExchangeRateRecordRepository.class);
        service = new ForeignExchangeEngineService(repository);
    }

    @Test
    void setsRateAndCalculatesFxGainLossSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExchangeRateRecord record = service.setRate("USD", "EGP", new BigDecimal("50.000000"), LocalDate.of(2026, 8, 1));
        assertThat(record).isNotNull();
        assertThat(record.getRate()).isEqualByComparingTo(new BigDecimal("50.000000"));

        ForeignExchangeEngineService.FxCalculationResult result = service.calculateGainLoss(
                new BigDecimal("100.00"),
                new BigDecimal("48.000000"),
                new BigDecimal("50.000000")
        );

        assertThat(result.originalBaseAmount()).isEqualByComparingTo(new BigDecimal("4800.00"));
        assertThat(result.currentBaseAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.gainLossAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(result.isGain()).isTrue();
    }
}
