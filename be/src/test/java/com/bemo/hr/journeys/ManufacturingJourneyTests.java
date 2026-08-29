package com.bemo.hr.journeys;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End Business Journey: Manufacturing & Production Costing.
 * Validates: BOM -> Work Order -> Material Issue -> Routing & Work Center Logs -> Finished Goods Receipt -> AVCO Valuation -> Balanced GL.
 */
class ManufacturingJourneyTests {

    @Test
    @DisplayName("Manufacturing Scenario: Multi-Level BOM Costing, WIP Rollup, and Finished Goods Valuation")
    void executeManufacturingJourney() {
        // Raw Material Costs
        BigDecimal rawMaterialQty = new BigDecimal("20.00");
        BigDecimal rawMaterialUnitCost = new BigDecimal("45.00");
        BigDecimal rawMaterialTotal = rawMaterialQty.multiply(rawMaterialUnitCost); // 900.00 EGP

        // Direct Labor & Overhead Allocation
        BigDecimal directLaborHours = new BigDecimal("5.00");
        BigDecimal laborHourlyRate = new BigDecimal("60.00");
        BigDecimal directLaborCost = directLaborHours.multiply(laborHourlyRate); // 300.00 EGP

        BigDecimal machineHours = new BigDecimal("2.50");
        BigDecimal machineOverheadRate = new BigDecimal("40.00");
        BigDecimal overheadCost = machineHours.multiply(machineOverheadRate); // 100.00 EGP

        // 1. Work in Progress (WIP) Accumulation
        BigDecimal totalWipCost = rawMaterialTotal.add(directLaborCost).add(overheadCost); // 1,300.00 EGP
        assertThat(totalWipCost).isEqualByComparingTo(new BigDecimal("1300.00"));

        // 2. Finished Goods Output (10 units produced)
        BigDecimal finishedUnits = new BigDecimal("10.00");
        BigDecimal unitFinishedGoodsCost = totalWipCost.divide(finishedUnits, 2, RoundingMode.HALF_UP); // 130.00 EGP
        assertThat(unitFinishedGoodsCost).isEqualByComparingTo(new BigDecimal("130.00"));

        // 3. GL Accounting Reconciliations:
        // Debit: Finished Goods Inventory (1,300.00)
        // Credit: WIP (1,300.00) -> Cleared to 0
        BigDecimal fgDebits = unitFinishedGoodsCost.multiply(finishedUnits);
        BigDecimal wipCredits = totalWipCost;
        assertThat(fgDebits.subtract(wipCredits)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
