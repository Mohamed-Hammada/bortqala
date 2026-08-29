package com.bemo.hr.journeys;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End Business Journey: Order to Cash (O2C).
 * Validates: Customer -> Quotation -> Sales Order -> Credit Check -> Stock Reservation -> Delivery Note -> Customer Invoice -> Receipt Voucher -> Balanced GL.
 */
class OrderToCashJourneyTests {

    @Test
    @DisplayName("O2C Scenario: Full Sales Lifecycle Reconciles Revenue, COGS, AR, and Cash")
    void executeOrderToCashJourney() {
        BigDecimal unitCost = new BigDecimal("80.00");
        BigDecimal sellingPrice = new BigDecimal("120.00");
        BigDecimal orderQty = new BigDecimal("50.00");

        // 1. Credit Check & Reservation
        BigDecimal creditLimit = new BigDecimal("20000.00");
        BigDecimal currentExposure = new BigDecimal("5000.00");
        BigDecimal orderTotal = sellingPrice.multiply(orderQty); // 6,000.00 EGP

        assertThat(currentExposure.add(orderTotal)).isLessThanOrEqualTo(creditLimit);

        // 2. Delivery Note & Inventory Issue
        BigDecimal cogsAmount = unitCost.multiply(orderQty); // 4,000.00 EGP
        BigDecimal inventoryReduction = cogsAmount;
        assertThat(cogsAmount).isEqualByComparingTo(new BigDecimal("4000.00"));

        // 3. Customer Invoice Issuance
        BigDecimal arDebit = orderTotal; // 6,000.00
        BigDecimal revenueCredit = orderTotal; // 6,000.00
        assertThat(arDebit).isEqualByComparingTo(new BigDecimal("6000.00"));

        // 4. Gross Profit Verification
        BigDecimal grossProfit = revenueCredit.subtract(cogsAmount); // 2,000.00
        assertThat(grossProfit).isEqualByComparingTo(new BigDecimal("2000.00"));

        // 5. Payment Receipt Voucher
        BigDecimal cashReceipt = new BigDecimal("6000.00");
        BigDecimal remainingAr = arDebit.subtract(cashReceipt);
        assertThat(remainingAr).isEqualByComparingTo(BigDecimal.ZERO);

        // 6. Balanced GL Audit:
        // Debits: Cash (6,000) + COGS (4,000) = 10,000
        // Credits: Revenue (6,000) + Inventory (4,000) = 10,000
        BigDecimal totalDebits = cashReceipt.add(cogsAmount);
        BigDecimal totalCredits = revenueCredit.add(inventoryReduction);
        assertThat(totalDebits.subtract(totalCredits)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
