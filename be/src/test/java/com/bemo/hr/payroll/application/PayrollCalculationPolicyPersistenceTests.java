package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.infrastructure.PayrollCalculationPolicyRepository;
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
class PayrollCalculationPolicyPersistenceTests {

    private final List<String> tenantIds = new ArrayList<>();
    @Autowired
    private PayrollCalculationPolicyService payrollCalculationPolicyService;
    @Autowired
    private PayrollCalculationPolicyRepository payrollCalculationPolicyRepository;
    @Autowired
    private TenantApplicationRepository tenantApplicationRepository;

    @AfterEach
    void cleanup() {
        try {
            for (String tenantId : tenantIds) {
                TenantContext.set(tenantId);
                payrollCalculationPolicyRepository.deleteAll();
            }
            TenantContext.clear();
            tenantApplicationRepository.deleteAllById(tenantIds);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void effectivePolicySelectionIsTenantScoped() {
        String tenantA = tenant("POL-A");
        String tenantB = tenant("POL-B");

        TenantContext.set(tenantA);
        payrollCalculationPolicyService.create("Tenant A", LocalDate.of(2026, 1, 1), null,
                new BigDecimal("200"), new BigDecimal("2"));
        assertThat(payrollCalculationPolicyService.effectivePolicy(LocalDate.of(2026, 8, 31)).getName())
                .isEqualTo("Tenant A");

        TenantContext.set(tenantB);
        assertThat(payrollCalculationPolicyService.list()).isEmpty();
        assertThat(payrollCalculationPolicyService.effectivePolicy(LocalDate.of(2026, 8, 31)).getName())
                .isEqualTo("Initial standard payroll policy");

        TenantContext.set(tenantA);
        assertThat(payrollCalculationPolicyService.list()).extracting("name").containsExactly("Tenant A");
    }

    private String tenant(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var tenant = tenantApplicationRepository.save(new TenantApplication(prefix + suffix, prefix + suffix));
        tenantIds.add(tenant.getId());
        return tenant.getId();
    }
}
