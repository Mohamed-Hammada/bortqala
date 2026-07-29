package com.bemo.hr.workforce;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WorkforceSettlementPeriodTests {
    @Test
    void failedRecalculationKeepsLastSuccessfulResult() {
        WorkforceSettlementPeriod period = new WorkforceSettlementPeriod("JUL-2", "2026-07-16", "2026-07-31", "HALF_MONTH", "DRAFT");
        period.markCalculated("admin", "fingerprint", 12, new BigDecimal("1000"),
                new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("850"), 2, 0);

        period.markCalculationFailed("تعذر قراءة سجل حضور");

        assertThat(period.getStatus()).isEqualTo("CALCULATED");
        assertThat(period.getCalculationVersion()).isEqualTo(1);
        assertThat(period.getResultNetAmount()).isEqualByComparingTo("850");
        assertThat(period.getLastCalculationError()).contains("تعذر قراءة سجل حضور");
    }
}
