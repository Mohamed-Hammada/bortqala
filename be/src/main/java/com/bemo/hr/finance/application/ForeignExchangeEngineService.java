package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.ExchangeRateRecord;
import com.bemo.hr.finance.infrastructure.ExchangeRateRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class ForeignExchangeEngineService {

    private final ExchangeRateRecordRepository rateRepository;

    public ForeignExchangeEngineService(ExchangeRateRecordRepository rateRepository) {
        this.rateRepository = rateRepository;
    }

    public record FxCalculationResult(
            BigDecimal foreignAmount,
            BigDecimal transactionRate,
            BigDecimal currentRate,
            BigDecimal originalBaseAmount,
            BigDecimal currentBaseAmount,
            BigDecimal gainLossAmount,
            boolean isGain
    ) {}

    @Transactional
    public ExchangeRateRecord setRate(String fromCurrency, String toCurrency, BigDecimal rate, LocalDate effectiveDate) {
        ExchangeRateRecord record = rateRepository.findByFromCurrencyAndToCurrencyAndEffectiveDate(fromCurrency, toCurrency, effectiveDate)
                .orElseGet(() -> new ExchangeRateRecord(fromCurrency, toCurrency, rate, effectiveDate));
        return rateRepository.save(record);
    }

    @Transactional(readOnly = true)
    public FxCalculationResult calculateGainLoss(BigDecimal foreignAmount, BigDecimal transactionRate, BigDecimal currentRate) {
        BigDecimal originalBase = foreignAmount.multiply(transactionRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentBase = foreignAmount.multiply(currentRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gainLoss = currentBase.subtract(originalBase);
        boolean isGain = gainLoss.compareTo(BigDecimal.ZERO) >= 0;

        return new FxCalculationResult(foreignAmount, transactionRate, currentRate, originalBase, currentBase, gainLoss, isGain);
    }
}
