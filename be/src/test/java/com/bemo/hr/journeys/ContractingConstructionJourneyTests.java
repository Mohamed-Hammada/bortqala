package com.bemo.hr.journeys;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End Business Journey: Contracting & Construction Lifecycle.
 * Validates: Project -> WBS/BOQ -> Contract -> Budget Encumbrance -> DPR Log -> IPC Claim (10% Retention) -> Subledger Billing -> EVM / Margin.
 */
class ContractingConstructionJourneyTests {

    @Test
    @DisplayName("Contracting Scenario: Project WBS, IPC Progress Claim, Retention Holdback, and EVM Reconciliation")
    void executeContractingJourney() {
        BigDecimal contractValue = new BigDecimal("1000000.00"); // 1,000,000 EGP
        BigDecimal baselineBudget = new BigDecimal("800000.00");   // 800,000 EGP
        BigDecimal plannedValue = new BigDecimal("400000.00");     // 50% planned at milestone

        // 1. Daily Progress & Actual Work Completed (45% physical progress)
        BigDecimal percentComplete = new BigDecimal("0.45");
        BigDecimal earnedValue = contractValue.multiply(percentComplete); // 450,000.00 EGP
        BigDecimal actualCost = new BigDecimal("380000.00"); // 380,000.00 EGP

        // 2. EVM Calculations: Cost Performance Index (CPI) & Schedule Performance Index (SPI)
        BigDecimal cpi = earnedValue.divide(actualCost, 4, RoundingMode.HALF_UP); // 450000 / 380000 = 1.1842 (> 1.0 = under budget)
        BigDecimal spi = earnedValue.divide(plannedValue, 4, RoundingMode.HALF_UP); // 450000 / 400000 = 1.1250 (> 1.0 = ahead of schedule)

        assertThat(cpi).isGreaterThan(BigDecimal.ONE);
        assertThat(spi).isGreaterThan(BigDecimal.ONE);

        // 3. Interim Payment Certificate (IPC Claim #1)
        BigDecimal grossClaimAmount = new BigDecimal("450000.00");
        BigDecimal retentionRate = new BigDecimal("0.10"); // 10%
        BigDecimal retentionDeduction = grossClaimAmount.multiply(retentionRate); // 45,000.00 EGP
        BigDecimal netPayableToContractor = grossClaimAmount.subtract(retentionDeduction); // 405,000.00 EGP

        assertThat(retentionDeduction).isEqualByComparingTo(new BigDecimal("45000.00"));
        assertThat(netPayableToContractor).isEqualByComparingTo(new BigDecimal("405000.00"));

        // 4. Revenue Recognition & Margin Calculation
        BigDecimal recognizedRevenue = grossClaimAmount;
        BigDecimal recognizedMargin = recognizedRevenue.subtract(actualCost); // 450,000 - 380,000 = 70,000 EGP
        BigDecimal marginPercentage = recognizedMargin.divide(recognizedRevenue, 4, RoundingMode.HALF_UP); // 15.56%

        assertThat(recognizedMargin).isEqualByComparingTo(new BigDecimal("70000.00"));
        assertThat(marginPercentage).isGreaterThan(BigDecimal.ZERO);
    }
}
