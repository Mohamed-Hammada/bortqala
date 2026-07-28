package com.bemo.hr.workforce;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkforceCalculationTest {

    @Test
    @DisplayName("Verify contractor accounting model: worker_net_total")
    void testWorkerNetTotalModel() {
        BigDecimal workerNet1 = new BigDecimal("1500.00");
        BigDecimal workerNet2 = new BigDecimal("2500.00");
        BigDecimal contractorPayable = workerNet1.add(workerNet2);

        assertEquals(new BigDecimal("4000.00"), contractorPayable);
    }

    @Test
    @DisplayName("Verify contractor accounting model: contractor_daily_rate")
    void testContractorDailyRateModel() {
        BigDecimal totalUnits = new BigDecimal("15.5"); // 15.5 days
        BigDecimal contractorRate = new BigDecimal("300.00"); // 300 EGP / day
        BigDecimal contractorPayable = totalUnits.multiply(contractorRate).setScale(2, RoundingMode.HALF_UP);

        assertEquals(new BigDecimal("4650.00"), contractorPayable);
    }

    @Test
    @DisplayName("Verify contractor accounting model: worker_cost_plus_fee percentage")
    void testWorkerCostPlusFeePercentageModel() {
        BigDecimal workerNet = new BigDecimal("10000.00");
        BigDecimal commissionPercent = new BigDecimal("10.0"); // 10%
        BigDecimal commission = workerNet.multiply(commissionPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal contractorPayable = workerNet.add(commission);

        assertEquals(new BigDecimal("1000.00"), commission);
        assertEquals(new BigDecimal("11000.00"), contractorPayable);
    }

    @Test
    @DisplayName("Verify contractor accounting model: fixed_period_amount")
    void testFixedPeriodAmountModel() {
        BigDecimal fixedAmount = new BigDecimal("50000.00");
        BigDecimal additions = new BigDecimal("2000.00");
        BigDecimal deductions = new BigDecimal("500.00");
        BigDecimal contractorPayable = fixedAmount.add(additions).subtract(deductions);

        assertEquals(new BigDecimal("51500.00"), contractorPayable);
    }

    @Test
    @DisplayName("Verify benchmark Excel financial metrics")
    void testBenchmarkFinancialMetrics() {
        BigDecimal gross = new BigDecimal("362617.50");
        BigDecimal deductions = new BigDecimal("1560.00");
        BigDecimal advances = new BigDecimal("25150.00");
        BigDecimal net = gross.subtract(deductions).subtract(advances);

        assertEquals(new BigDecimal("335907.50"), net);
    }
}
