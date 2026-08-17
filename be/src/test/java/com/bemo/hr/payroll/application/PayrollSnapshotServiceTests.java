package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollInputSnapshot;
import com.bemo.hr.payroll.infrastructure.PayrollInputSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PayrollSnapshotServiceTests {

    private PayrollInputSnapshotRepository snapshotRepository;
    private PayrollSnapshotService snapshotService;

    @BeforeEach
    void setUp() {
        snapshotRepository = mock(PayrollInputSnapshotRepository.class);
        snapshotService = new PayrollSnapshotService(snapshotRepository);
    }

    @Test
    void capturesNewPayrollSnapshotSuccessfully() {
        when(snapshotRepository.findByPayrollRunIdAndEmployeeId("run-1", "emp-1")).thenReturn(Optional.empty());
        when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PayrollInputSnapshot snapshot = snapshotService.captureSnapshot(new PayrollSnapshotService.CalculationInputs(
                "run-1", "emp-1", "p-1", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("5000.00"), 9600, 600, 60, 0, "policy-1", 3,
                new BigDecimal("200.00"), new BigDecimal("2.00"), new BigDecimal("175.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), "payroll_admin");

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getEmployeeId()).isEqualTo("emp-1");
        assertThat(snapshot.getPeriodId()).isEqualTo("p-1");
        assertThat(snapshot.getNetPay()).isEqualTo(new BigDecimal("5300.00"));
        assertThat(snapshot.getWorkingHourDivisor()).isEqualByComparingTo("200");
        assertThat(snapshot.getLockedBy()).isEqualTo("payroll_admin");
    }

    @Test
    void repeatedCalculationUsesFrozenSnapshotInsteadOfChangedSalaryAttendanceAndPolicy() {
        PayrollSnapshotService.CalculationInputs original = new PayrollSnapshotService.CalculationInputs(
                "run-1", "emp-1", "2026-08:FULL_MONTH", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("5000"), 9000, 120, 60, 0, "policy-1", 1,
                new BigDecimal("240"), new BigDecimal("1.5"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
        when(snapshotRepository.findByPayrollRunIdAndEmployeeId(original.payrollRunId(), original.employeeId()))
                .thenReturn(Optional.empty());
        when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        PayrollInputSnapshot frozen = snapshotService.captureSnapshot(original, "payroll");

        when(snapshotRepository.findByPayrollRunIdAndEmployeeId(original.payrollRunId(), original.employeeId()))
                .thenReturn(Optional.of(frozen));
        PayrollSnapshotService.CalculationInputs changed = new PayrollSnapshotService.CalculationInputs(
                original.payrollRunId(), original.employeeId(), original.periodId(), original.periodStart(), original.periodEnd(),
                new BigDecimal("9000"), 100, 900, 500, 0, "policy-2", 9,
                new BigDecimal("100"), new BigDecimal("3"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);

        PayrollInputSnapshot replay = snapshotService.captureSnapshot(changed, "payroll");

        assertThat(replay).isSameAs(frozen);
        assertThat(replay.getBaseSalary()).isEqualByComparingTo("5000");
        assertThat(replay.getOvertimeMinutes()).isEqualTo(120);
        assertThat(replay.getPayrollPolicyId()).isEqualTo("policy-1");
        verify(snapshotRepository, times(1)).save(any());
    }

    @Test
    void configuredDivisorAndOvertimeMultiplierDriveTheFrozenResult() {
        when(snapshotRepository.findByPayrollRunIdAndEmployeeId(anyString(), anyString())).thenReturn(Optional.empty());
        when(snapshotRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PayrollInputSnapshot standard = snapshotService.captureSnapshot(new PayrollSnapshotService.CalculationInputs(
                "run-standard", "emp-1", "p-1", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("4800"), 9600, 600, 0, 0, "policy-standard", 1,
                new BigDecimal("240"), new BigDecimal("1.5"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO), "payroll");
        PayrollInputSnapshot configured = snapshotService.captureSnapshot(new PayrollSnapshotService.CalculationInputs(
                "run-configured", "emp-1", "p-1", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("4800"), 9600, 600, 0, 0, "policy-configured", 1,
                new BigDecimal("200"), new BigDecimal("2"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO), "payroll");

        assertThat(standard.getAllowanceAmount()).isEqualByComparingTo("300.00");
        assertThat(configured.getAllowanceAmount()).isEqualByComparingTo("480.00");
        assertThat(configured.getWorkingHourDivisor()).isEqualByComparingTo("200");
        assertThat(configured.getOvertimeMultiplier()).isEqualByComparingTo("2");
    }
}
