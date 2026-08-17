package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.infrastructure.PayrollInputSnapshotRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PayrollSnapshotPersistenceTests {

    private final List<String> tenantIds = new ArrayList<>();
    @Autowired
    private PayrollSnapshotService snapshotService;
    @Autowired
    private PayrollInputSnapshotRepository snapshotRepository;
    @Autowired
    private TenantApplicationRepository tenantApplicationRepository;

    @AfterEach
    void cleanup() {
        try {
            for (String tenantId : tenantIds) {
                TenantContext.set(tenantId);
                snapshotRepository.deleteAll();
            }
            TenantContext.clear();
            tenantApplicationRepository.deleteAllById(tenantIds);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void frozenRunReplaysItsInputsNewRunUsesNewInputsAndTenantsAreIsolated() {
        String tenantA = tenant("PAY-A");
        String tenantB = tenant("PAY-B");

        TenantContext.set(tenantA);
        var first = snapshotService.captureSnapshot(inputs("run-1", "emp-1", "5000", 120, "policy-1"), "maker");
        var replay = snapshotService.captureSnapshot(inputs("run-1", "emp-1", "9000", 900, "policy-2"), "maker");
        var nextRun = snapshotService.captureSnapshot(inputs("run-2", "emp-1", "9000", 900, "policy-2"), "maker");

        assertThat(replay.getId()).isEqualTo(first.getId());
        assertThat(replay.getBaseSalary()).isEqualByComparingTo("5000");
        assertThat(nextRun.getId()).isNotEqualTo(first.getId());
        assertThat(nextRun.getBaseSalary()).isEqualByComparingTo("9000");
        assertThat(snapshotRepository.findByPayrollRunIdAndEmployeeId("run-1", "emp-1")).isPresent();

        TenantContext.set(tenantB);
        assertThat(snapshotRepository.findByPayrollRunIdAndEmployeeId("run-1", "emp-1")).isEmpty();
    }

    private PayrollSnapshotService.CalculationInputs inputs(String runId, String employeeId, String salary,
                                                            long overtimeMinutes, String policyId) {
        return new PayrollSnapshotService.CalculationInputs(runId, employeeId, "2026-08:FULL_MONTH",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), new BigDecimal(salary),
                9600, overtimeMinutes, 60, 1, policyId, 1, new BigDecimal("240"),
                new BigDecimal("1.5"), new BigDecimal("50"), new BigDecimal("25"),
                new BigDecimal("100"), new BigDecimal("100"));
    }

    private String tenant(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var tenant = tenantApplicationRepository.save(new TenantApplication(prefix + suffix, prefix + suffix));
        tenantIds.add(tenant.getId());
        return tenant.getId();
    }
}
