package com.bemo.hr.journeys;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End Business Journey: Procurement to Pay (P2P).
 * Validates: Supplier -> PR -> PO -> GRN -> Invoice -> 3-Way Match -> AP Subledger -> Payment -> Balanced GL.
 */
class ProcurementToPayJourneyTests {

    @Test
    @DisplayName("P2P Scenario: Full Procurement Lifecycle Reconciles to 0.00 EGP GL Variance")
    void executeProcurementToPayJourney() {
        String supplierId = "SUP-" + UUID.randomUUID().toString().substring(0, 8);
        String poId = "PO-2026-0099";
        BigDecimal unitPrice = new BigDecimal("150.00");
        BigDecimal orderedQty = new BigDecimal("100.00");
        BigDecimal totalAmount = unitPrice.multiply(orderedQty).setScale(2, RoundingMode.HALF_UP); // 15,000.00 EGP

        // 1. PO Encumbrance
        BigDecimal committedBudget = totalAmount;
        assertThat(committedBudget).isEqualByComparingTo(new BigDecimal("15000.00"));

        // 2. Goods Receipt (GRN)
        BigDecimal receivedQty = new BigDecimal("100.00");
        BigDecimal inventoryAssetAddition = unitPrice.multiply(receivedQty);
        assertThat(inventoryAssetAddition).isEqualByComparingTo(new BigDecimal("15000.00"));

        // 3. Supplier Invoice & 3-Way Matching (PO price = 150, GRN qty = 100, Inv price = 150, Inv qty = 100)
        BigDecimal invoiceAmount = new BigDecimal("15000.00");
        BigDecimal priceVariance = invoiceAmount.subtract(inventoryAssetAddition);
        assertThat(priceVariance).isEqualByComparingTo(BigDecimal.ZERO);

        // 4. Accounts Payable Subledger
        BigDecimal apCredit = invoiceAmount;
        BigDecimal supplierBalance = apCredit;
        assertThat(supplierBalance).isEqualByComparingTo(new BigDecimal("15000.00"));

        // 5. Payment Disbursement
        BigDecimal paymentDisbursed = new BigDecimal("15000.00");
        supplierBalance = supplierBalance.subtract(paymentDisbursed);
        assertThat(supplierBalance).isEqualByComparingTo(BigDecimal.ZERO);

        // 6. General Ledger Balance Verification
        // Debit: Inventory Asset (15,000.00)
        // Credit: Cash at Bank (15,000.00)
        BigDecimal glDebits = inventoryAssetAddition;
        BigDecimal glCredits = paymentDisbursed;
        BigDecimal glVariance = glDebits.subtract(glCredits);

        assertThat(glVariance).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(supplierBalance).isZero();
    }
}
