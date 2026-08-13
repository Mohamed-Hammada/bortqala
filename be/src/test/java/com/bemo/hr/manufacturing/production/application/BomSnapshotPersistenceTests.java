package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.infrastructure.BomSnapshotRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BomSnapshotPersistenceTests {

    @Autowired private BomSnapshotService bomSnapshotService;
    @Autowired private BomSnapshotRepository bomSnapshotRepository;
    @Autowired private TenantApplicationRepository tenantApplicationRepository;

    private final List<String> tenantIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        try {
            for (String tenantId : tenantIds) {
                TenantContext.set(tenantId);
                bomSnapshotRepository.deleteAll();
            }
            TenantContext.clear();
            tenantApplicationRepository.deleteAllById(tenantIds);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void frozenRequirementsReplayR1NewOrderUsesR2AndTenantsAreIsolated() {
        String tenantA = tenant("MFG-A");
        String tenantB = tenant("MFG-B");

        TenantContext.set(tenantA);
        var r1 = bomSnapshotService.captureBomSnapshot("wo-1", "bom-1", 1, "rm-1",
                new BigDecimal("10"), new BigDecimal("15"));
        var replay = bomSnapshotService.captureBomSnapshot("wo-1", "bom-1", 2, "rm-1",
                new BigDecimal("99"), new BigDecimal("40"));
        var r2 = bomSnapshotService.captureBomSnapshot("wo-2", "bom-1", 2, "rm-1",
                new BigDecimal("99"), new BigDecimal("40"));

        assertThat(replay.getId()).isEqualTo(r1.getId());
        assertThat(replay.getBomVersion()).isEqualTo(1);
        assertThat(replay.getRequiredQuantity()).isEqualByComparingTo("10");
        assertThat(replay.getStandardUnitCost()).isEqualByComparingTo("15");
        assertThat(r2.getBomVersion()).isEqualTo(2);
        assertThat(r2.getRequiredQuantity()).isEqualByComparingTo("99");

        TenantContext.set(tenantB);
        assertThat(bomSnapshotService.getSnapshotsForProductionOrder("wo-1")).isEmpty();
    }

    private String tenant(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var tenant = tenantApplicationRepository.save(new TenantApplication(prefix + suffix, prefix + suffix));
        tenantIds.add(tenant.getId());
        return tenant.getId();
    }
}
