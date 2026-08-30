package com.bemo.hr.journeys;

import com.bemo.hr.payroll.application.PayrollSnapshotService;
import com.bemo.hr.payroll.domain.PayrollInputSnapshot;
import com.bemo.hr.payroll.infrastructure.PayrollInputSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Journey: Payroll Frozen Snapshot Immutability & Recalculation Guard")
class PayrollFrozenSnapshotImmutabilityJourneyTests {

    private PayrollInputSnapshotRepository snapshotRepository;
    private PayrollSnapshotService snapshotService;

    @BeforeEach
    void setUp() {
        snapshotRepository = mock(PayrollInputSnapshotRepository.class);
        snapshotService = new PayrollSnapshotService(snapshotRepository);
    }

    @Test
    @DisplayName("Freeze Day 1 (10k + 22d + P1) -> Mutate Live Data to 15k -> Existing Run Remains 10k -> New Run Uses 15k")
    void provesFrozenSnapshotImmunityAgainstLiveEmployeeAndPolicyMutations() {
        // --- Step 1: Day 1 Baseline (Salary 10,000 EGP, Policy P1 with 200 divisor, 1.5 OT multiplier) ---
        String runAugust = "RUN-2026-08";
        String empId = "EMP-1001";
        PayrollSnapshotService.CalculationInputs augustInputs = new PayrollSnapshotService.CalculationInputs(
                runAugust,
                empId,
                "2026-08:FULL_MONTH",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                new BigDecimal("10000.00"),
                10560, // 22 days * 8 hours * 60 mins
                300,   // 5 hours overtime
                0,     // 0 late mins
                0,     // 0 absence
                "POLICY-P1",
                1L,
                new BigDecimal("200.00"),
                new BigDecimal("1.5000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        when(snapshotRepository.findByPayrollRunIdAndEmployeeId(runAugust, empId)).thenReturn(Optional.empty());
        when(snapshotRepository.save(any(PayrollInputSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        PayrollInputSnapshot frozenAugust = snapshotService.captureSnapshot(augustInputs, "payroll_admin");

        assertThat(frozenAugust.getBaseSalary()).isEqualByComparingTo("10000.00");
        assertThat(frozenAugust.getPayrollPolicyId()).isEqualTo("POLICY-P1");
        // Overtime = (10000 / 200) * (300 / 60) * 1.5 = 50 * 5 * 1.5 = 375.00 EGP
        assertThat(frozenAugust.getAllowanceAmount()).isEqualByComparingTo("375.00");
        assertThat(frozenAugust.getGrossPay()).isEqualByComparingTo("10375.00");
        assertThat(frozenAugust.getNetPay()).isEqualByComparingTo("10375.00");

        // --- Step 2: Live Employee & Policy Mutation ---
        // Live Salary changed to 15,000 EGP, Overtime doubled to 10 hours, Policy updated to P2 (divisor 180, OT mult 2.0)
        when(snapshotRepository.findByPayrollRunIdAndEmployeeId(runAugust, empId)).thenReturn(Optional.of(frozenAugust));

        PayrollSnapshotService.CalculationInputs mutatedLiveAugustInputs = new PayrollSnapshotService.CalculationInputs(
                runAugust,
                empId,
                "2026-08:FULL_MONTH",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                new BigDecimal("15000.00"), // Mutated salary
                10560,
                600, // 10 hours overtime
                120, // 2 hours late
                2,   // 2 absence days
                "POLICY-P2",
                2L,
                new BigDecimal("180.00"),
                new BigDecimal("2.0000"),
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        // --- Step 3: Recalculate August Run -> MUST return the frozen snapshot untouched ---
        PayrollInputSnapshot recalculatedAugust = snapshotService.captureSnapshot(mutatedLiveAugustInputs, "payroll_admin");

        assertThat(recalculatedAugust).isSameAs(frozenAugust);
        assertThat(recalculatedAugust.getBaseSalary()).isEqualByComparingTo("10000.00");
        assertThat(recalculatedAugust.getPayrollPolicyId()).isEqualTo("POLICY-P1");
        assertThat(recalculatedAugust.getAllowanceAmount()).isEqualByComparingTo("375.00");
        assertThat(recalculatedAugust.getGrossPay()).isEqualByComparingTo("10375.00");
        assertThat(recalculatedAugust.getNetPay()).isEqualByComparingTo("10375.00");

        // --- Step 4: Create September Run (New Run) -> Takes the new live 15,000 EGP and Policy P2 ---
        String runSeptember = "RUN-2026-09";
        when(snapshotRepository.findByPayrollRunIdAndEmployeeId(runSeptember, empId)).thenReturn(Optional.empty());

        PayrollSnapshotService.CalculationInputs septemberInputs = new PayrollSnapshotService.CalculationInputs(
                runSeptember,
                empId,
                "2026-09:FULL_MONTH",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                new BigDecimal("15000.00"),
                10560,
                300, // 5 hours overtime
                0,
                0,
                "POLICY-P2",
                2L,
                new BigDecimal("200.00"),
                new BigDecimal("2.0000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        PayrollInputSnapshot frozenSeptember = snapshotService.captureSnapshot(septemberInputs, "payroll_admin");

        assertThat(frozenSeptember.getBaseSalary()).isEqualByComparingTo("15000.00");
        assertThat(frozenSeptember.getPayrollPolicyId()).isEqualTo("POLICY-P2");
        // Overtime = (15000 / 200) * (300 / 60) * 2.0 = 75 * 5 * 2.0 = 750.00 EGP
        assertThat(frozenSeptember.getAllowanceAmount()).isEqualByComparingTo("750.00");
        assertThat(frozenSeptember.getGrossPay()).isEqualByComparingTo("15750.00");
        assertThat(frozenSeptember.getNetPay()).isEqualByComparingTo("15750.00");

        // Verify that snapshot repository was saved exactly twice (once for Aug, once for Sep)
        verify(snapshotRepository, times(2)).save(any(PayrollInputSnapshot.class));
    }
}
