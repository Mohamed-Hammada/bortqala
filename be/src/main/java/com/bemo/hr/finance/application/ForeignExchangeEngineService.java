package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.ExchangeRateRecord;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.finance.domain.FxPosting;
import com.bemo.hr.finance.domain.posting.SubledgerPostingService;
import com.bemo.hr.finance.infrastructure.ExchangeRateRecordRepository;
import com.bemo.hr.finance.infrastructure.FxPostingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Slf4j
@Service
public class ForeignExchangeEngineService {

    private final ExchangeRateRecordRepository rateRepository;
    private final FxPostingRepository fxPostingRepository;
    private final SubledgerPostingService subledgerPostingService;
    private final FiscalPeriodGuard fiscalPeriodGuard;

    public ForeignExchangeEngineService(ExchangeRateRecordRepository rateRepository) {
        this(rateRepository, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ForeignExchangeEngineService(ExchangeRateRecordRepository rateRepository, FxPostingRepository fxPostingRepository,
                                        SubledgerPostingService subledgerPostingService, FiscalPeriodGuard fiscalPeriodGuard) {
        this.rateRepository = rateRepository;
        this.fxPostingRepository = fxPostingRepository;
        this.subledgerPostingService = subledgerPostingService;
        this.fiscalPeriodGuard = fiscalPeriodGuard;
    }

    @Transactional
    public ExchangeRateRecord setRate(String fromCurrency, String toCurrency, BigDecimal rate, LocalDate effectiveDate) {
        log.debug("setRate called with fromCurrency={}, toCurrency={}, rate={}, effectiveDate={}", fromCurrency, toCurrency, rate, effectiveDate);
        ExchangeRateRecord record = rateRepository.findByFromCurrencyAndToCurrencyAndEffectiveDate(fromCurrency, toCurrency, effectiveDate)
                .orElseGet(() -> new ExchangeRateRecord(fromCurrency, toCurrency, rate, effectiveDate));
        ExchangeRateRecord saved = rateRepository.save(record);
        log.info("ExchangeRateRecord saved for {}/{} effective {}", fromCurrency, toCurrency, effectiveDate);
        return saved;
    }

    @Transactional(readOnly = true)
    public FxCalculationResult calculateGainLoss(BigDecimal foreignAmount, BigDecimal transactionRate, BigDecimal currentRate) {
        log.debug("calculateGainLoss called with foreignAmount={}, transactionRate={}, currentRate={}", foreignAmount, transactionRate, currentRate);
        BigDecimal originalBase = foreignAmount.multiply(transactionRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentBase = foreignAmount.multiply(currentRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gainLoss = currentBase.subtract(originalBase);
        boolean isGain = gainLoss.compareTo(BigDecimal.ZERO) >= 0;

        return new FxCalculationResult(foreignAmount, transactionRate, currentRate, originalBase, currentBase, gainLoss, isGain);
    }

    @Transactional
    public FxPosting post(FxPosting.Type type, String sourceDocumentId, BigDecimal foreignAmount, BigDecimal transactionRate,
                          BigDecimal closingRate, String rateSource, LocalDate effectiveDate, String operationId) {
        log.debug("post called with type={}, sourceDocumentId={}, foreignAmount={}, operationId={}", type, sourceDocumentId, foreignAmount, operationId);
        FxPosting replay = fxPostingRepository.findByOperationId(operationId).orElse(null);
        if (replay != null) {
            log.debug("post replayed from existing operationId={}", operationId);
            return replay;
        }
        if (rateSource == null || rateSource.isBlank()) {
            log.warn("Validation failed: rateSource is required");
            throw new com.bemo.hr.shared.domain.BusinessRuleException("Exchange rate source is required.", "FX_RATE_SOURCE_REQUIRED", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        var period = fiscalPeriodGuard.requireOpen(effectiveDate);
        var calc = calculateGainLoss(foreignAmount, transactionRate, closingRate);
        BigDecimal amount = calc.gainLossAmount().abs();
        var journal = subledgerPostingService.postSubledgerEvent("FINANCE", "FX_" + type, sourceDocumentId, "FX_" + type, operationId + ":JOURNAL", effectiveDate, "FX " + type + " " + sourceDocumentId, amount, amount, period.getId());
        FxPosting saved = fxPostingRepository.save(new FxPosting(type, sourceDocumentId, foreignAmount, transactionRate, closingRate, calc.gainLossAmount(), rateSource, effectiveDate, period.getId(), journal.getId(), operationId));
        log.info("FxPosting {} posted successfully for sourceDocumentId={}", saved.getId(), sourceDocumentId);
        return saved;
    }

    @Transactional
    public FxPosting reverse(String id, String operationId, LocalDate date, String reason, String actor) {
        log.debug("reverse called with id={}, operationId={}, date={}", id, operationId, date);
        FxPosting posting = fxPostingRepository.findById(id).orElseThrow();
        if (posting.getStatus() == FxPosting.Status.REVERSED) {
            log.debug("FxPosting {} already reversed", id);
            return posting;
        }
        var journal = subledgerPostingService.reverse(posting.getJournalEntryId(), operationId, date, reason, actor);
        posting.reverse(journal.getId());
        FxPosting saved = fxPostingRepository.save(posting);
        log.info("FxPosting {} reversed successfully", id);
        return saved;
    }

    public record FxCalculationResult(
            BigDecimal foreignAmount,
            BigDecimal transactionRate,
            BigDecimal currentRate,
            BigDecimal originalBaseAmount,
            BigDecimal currentBaseAmount,
            BigDecimal gainLossAmount,
            boolean isGain
    ) {
    }
}
