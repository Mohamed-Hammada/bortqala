package com.bemo.hr.finance.application;

import com.bemo.hr.finance.api.ExchangeRateHintApi;
import com.bemo.hr.finance.domain.Currency;
import com.bemo.hr.finance.domain.ExchangeRateHintSetting;
import com.bemo.hr.finance.infrastructure.CurrencyRepository;
import com.bemo.hr.finance.infrastructure.ExchangeRateHintSettingRepository;
import com.bemo.hr.finance.infrastructure.FrankfurterExchangeRateClient;
import com.bemo.hr.finance.infrastructure.FrankfurterExchangeRateClient.RateRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class ExchangeRateHintService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateHintService.class);
    private static final String PROVIDER = "FRANKFURTER";

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateHintSettingRepository settingRepository;
    private final FrankfurterExchangeRateClient frankfurterClient;

    public ExchangeRateHintService(
            CurrencyRepository currencyRepository,
            ExchangeRateHintSettingRepository settingRepository,
            FrankfurterExchangeRateClient frankfurterClient) {
        this.currencyRepository = currencyRepository;
        this.settingRepository = settingRepository;
        this.frankfurterClient = frankfurterClient;
    }

    static BigDecimal toBaseRate(BigDecimal providerBaseToQuoteRate) {
        if (providerBaseToQuoteRate == null || providerBaseToQuoteRate.signum() <= 0) {
            throw new IllegalArgumentException("Provider rate must be greater than zero");
        }
        // Frankfurter: 1 BASE = X QUOTE.
        // ERP manual rate convention: 1 QUOTE = X BASE.
        return BigDecimal.ONE.divide(providerBaseToQuoteRate, 8, RoundingMode.HALF_UP);
    }

    @Transactional
    public ExchangeRateHintApi.SettingsResponse settings() {
        return toResponse(currentSetting());
    }

    @Transactional
    public ExchangeRateHintApi.SettingsResponse updateSettings(ExchangeRateHintApi.SettingsRequest request) {
        ExchangeRateHintSetting setting = currentSetting();
        setting.update(request.enabled(), request.refreshIntervalHours());
        return toResponse(setting);
    }

    @Transactional
    public ExchangeRateHintApi.RefreshResponse refreshNow() {
        ExchangeRateHintSetting setting = currentSetting();
        if (!setting.isEnabled()) {
            return failure(setting, "DISABLED", null);
        }
        return refresh(setting);
    }

    @Transactional
    public void refreshIfDue() {
        ExchangeRateHintSetting setting = currentSetting();
        if (!setting.isDue(System.currentTimeMillis())) return;
        refresh(setting);
    }

    private ExchangeRateHintApi.RefreshResponse refresh(ExchangeRateHintSetting setting) {
        long attemptedAt = System.currentTimeMillis();
        setting.recordAttempt(attemptedAt);

        List<Currency> activeCurrencies = currencyRepository.findAllByOrderByCodeAsc().stream()
                .filter(Currency::isActive)
                .toList();

        if (activeCurrencies.isEmpty()) {
            setting.recordFailure(attemptedAt, "NO_ACTIVE_CURRENCIES");
            return failure(setting, "NO_ACTIVE_CURRENCIES", null);
        }

        List<Currency> baseCurrencies = activeCurrencies.stream().filter(Currency::isBase).toList();
        if (baseCurrencies.isEmpty()) {
            setting.recordFailure(attemptedAt, "BASE_CURRENCY_REQUIRED");
            return failure(setting, "BASE_CURRENCY_REQUIRED", null);
        }
        if (baseCurrencies.size() > 1) {
            setting.recordFailure(attemptedAt, "MULTIPLE_BASE_CURRENCIES");
            return failure(setting, "MULTIPLE_BASE_CURRENCIES", null);
        }

        Currency baseCurrency = baseCurrencies.get(0);
        String baseCode = baseCurrency.getCode().toUpperCase(Locale.ROOT);

        try {
            Set<String> supported = frankfurterClient.supportedCurrencies();
            if (!supported.contains(baseCode)) {
                setting.recordFailure(attemptedAt, "BASE_CURRENCY_UNSUPPORTED");
                return failure(setting, "BASE_CURRENCY_UNSUPPORTED", baseCode);
            }

            List<String> quotes = activeCurrencies.stream()
                    .map(Currency::getCode)
                    .map(code -> code.toUpperCase(Locale.ROOT))
                    .filter(code -> !code.equals(baseCode))
                    .filter(supported::contains)
                    .toList();

            List<RateRow> rows = frankfurterClient.latestRates(baseCode, quotes);
            Map<String, RateRow> ratesByQuote = new HashMap<>();
            for (RateRow row : rows) {
                if (row.quote() != null) {
                    ratesByQuote.put(row.quote().toUpperCase(Locale.ROOT), row);
                }
            }

            LocalDate baseDate = rows.stream()
                    .map(RateRow::date)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(LocalDate.now(ZoneOffset.UTC));

            int refreshed = 0;
            int unsupported = 0;

            for (Currency currency : activeCurrencies) {
                String code = currency.getCode().toUpperCase(Locale.ROOT);

                if (code.equals(baseCode)) {
                    currency.updateReferenceRate(baseCode, BigDecimal.ONE.setScale(8), baseDate, attemptedAt);
                    refreshed++;
                    continue;
                }

                if (!supported.contains(code)) {
                    currency.markReferenceUnavailable(baseCode, attemptedAt, false);
                    unsupported++;
                    continue;
                }

                RateRow row = ratesByQuote.get(code);
                if (row == null || row.rate() == null || row.rate().signum() <= 0) {
                    currency.markReferenceUnavailable(baseCode, attemptedAt, true);
                    unsupported++;
                    continue;
                }

                currency.updateReferenceRate(baseCode, toBaseRate(row.rate()), row.date(), attemptedAt);
                refreshed++;
            }

            setting.recordSuccess(attemptedAt);
            return new ExchangeRateHintApi.RefreshResponse(
                    true, refreshed, unsupported, baseCode, attemptedAt, null
            );
        } catch (RuntimeException ex) {
            log.warn("Frankfurter exchange-rate hint refresh failed for base {}", baseCode, ex);
            setting.recordFailure(attemptedAt, "FRANKFURTER_UNAVAILABLE");
            return failure(setting, "FRANKFURTER_UNAVAILABLE", baseCode);
        }
    }

    private ExchangeRateHintSetting currentSetting() {
        return settingRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> settingRepository.save(ExchangeRateHintSetting.createDefault()));
    }

    private ExchangeRateHintApi.RefreshResponse failure(
            ExchangeRateHintSetting setting, String errorCode, String baseCurrency) {
        return new ExchangeRateHintApi.RefreshResponse(
                false, 0, 0, baseCurrency, setting.getLastAttemptAt(), errorCode
        );
    }

    private ExchangeRateHintApi.SettingsResponse toResponse(ExchangeRateHintSetting setting) {
        return new ExchangeRateHintApi.SettingsResponse(
                PROVIDER,
                setting.isEnabled(),
                setting.getRefreshIntervalHours(),
                setting.getLastAttemptAt(),
                setting.getLastSuccessAt(),
                setting.nextRefreshAt(),
                setting.getLastErrorCode()
        );
    }
}
