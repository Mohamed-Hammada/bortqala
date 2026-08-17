package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.*;
import com.bemo.hr.finance.domain.posting.SubledgerPostingService;
import com.bemo.hr.finance.infrastructure.ExchangeRateRecordRepository;
import com.bemo.hr.finance.infrastructure.FxPostingRepository;
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

        ForeignExchangeEngineService.FxCalculationResult loss = service.calculateGainLoss(
                new BigDecimal("100.00"), new BigDecimal("50.000000"), new BigDecimal("48.000000"));
        assertThat(loss.gainLossAmount()).isEqualByComparingTo(new BigDecimal("-200.00"));
        assertThat(loss.isGain()).isFalse();
    }

    @Test
    void postsGainAndLossReplaySafelyAndReversesLinkedJournal() {
        FxPostingRepository postings = mock(FxPostingRepository.class);
        SubledgerPostingService postingService = mock(SubledgerPostingService.class);
        FiscalPeriodGuard guard = mock(FiscalPeriodGuard.class);
        FiscalPeriod period = new FiscalPeriod(2026, 8, "August", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), FiscalPeriod.Status.OPEN);
        when(guard.requireOpen(LocalDate.of(2026, 8, 31))).thenReturn(period);
        when(postings.save(any())).thenAnswer(i -> i.getArgument(0));
        JournalEntry journal = new JournalEntry("FX-1", LocalDate.of(2026, 8, 31), "FX", "FX", null);
        when(postingService.postSubledgerEvent(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString(), any(), any(), anyString())).thenReturn(journal);
        service = new ForeignExchangeEngineService(repository, postings, postingService, guard);
        FxPosting gain = service.post(FxPosting.Type.UNREALIZED, "inv-1", new BigDecimal("100"), new BigDecimal("48"), new BigDecimal("50"), "CB-EGYPT", LocalDate.of(2026, 8, 31), "fx-1");
        assertThat(gain.getGainLossAmount()).isEqualByComparingTo("200");
        assertThat(gain.getRateSource()).isEqualTo("CB-EGYPT");
        when(postings.findByOperationId("fx-1")).thenReturn(java.util.Optional.of(gain));
        assertThat(service.post(FxPosting.Type.UNREALIZED, "inv-1", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "X", LocalDate.now(), "fx-1")).isSameAs(gain);
    }
}
