package com.bemo.hr.finance.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateHintServiceTest {

    @Test
    void convertsFrankfurterBaseToQuoteRateIntoErpQuoteToBaseHint() {
        // Frankfurter: 1 EGP = 0.02 USD.
        // ERP display convention: 1 USD = 50 EGP.
        assertThat(ExchangeRateHintService.toBaseRate(new BigDecimal("0.02000000")))
                .isEqualByComparingTo(new BigDecimal("50.00000000"));
    }

    @Test
    void rejectsZeroProviderRate() {
        assertThatThrownBy(() -> ExchangeRateHintService.toBaseRate(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
