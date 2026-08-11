package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollInputSnapshot;
import com.bemo.hr.payroll.infrastructure.PayrollInputSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
        when(snapshotRepository.findByEmployeeIdAndPeriodId("emp-1", "p-1")).thenReturn(Optional.empty());
        when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PayrollInputSnapshot snapshot = snapshotService.captureSnapshot(
                "emp-1",
                "p-1",
                new BigDecimal("160.00"),
                new BigDecimal("10.00"),
                0,
                new BigDecimal("200.00"),
                new BigDecimal("500.00"),
                new BigDecimal("5500.00"),
                new BigDecimal("5300.00"),
                "payroll_admin"
        );

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getEmployeeId()).isEqualTo("emp-1");
        assertThat(snapshot.getPeriodId()).isEqualTo("p-1");
        assertThat(snapshot.getNetPay()).isEqualTo(new BigDecimal("5300.00"));
        assertThat(snapshot.getLockedBy()).isEqualTo("payroll_admin");
    }
}
