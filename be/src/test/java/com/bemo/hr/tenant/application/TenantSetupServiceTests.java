package com.bemo.hr.tenant.application;

import com.bemo.hr.access.domain.SecurityPermission;
import com.bemo.hr.access.domain.SecurityPolicyGroup;
import com.bemo.hr.access.infrastructure.PolicyGroupPermissionRepository;
import com.bemo.hr.access.infrastructure.SecurityPermissionRepository;
import com.bemo.hr.access.infrastructure.SecurityPolicyGroupRepository;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.shared.security.TenantFeature;
import com.bemo.hr.shared.security.TenantFeatureRepository;
import com.bemo.hr.shared.security.TenantFeatureService;
import com.bemo.hr.tenant.api.TenantSetupApi.TenantVerticalResponse;
import com.bemo.hr.tenant.domain.BusinessVertical;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TenantSetupServiceTests {

    private final TenantFeatureRepository featureRepository = mock(TenantFeatureRepository.class);
    private final TenantFeatureService featureService = mock(TenantFeatureService.class);
    private final SecurityPolicyGroupRepository policyGroupRepository = mock(SecurityPolicyGroupRepository.class);
    private final PolicyGroupPermissionRepository groupPermissionRepository = mock(PolicyGroupPermissionRepository.class);
    private final SecurityPermissionRepository permissionRepository = mock(SecurityPermissionRepository.class);
    private final AuditService auditService = mock(AuditService.class);

    private TenantSetupService service;

    @BeforeEach
    void setUp() {
        TenantContext.set("tenant-abc");
        service = new TenantSetupService(
                featureRepository,
                featureService,
                policyGroupRepository,
                groupPermissionRepository,
                permissionRepository,
                auditService
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void configureTenantVertical_medical_enablesMedicalFeaturesAndProvisionsGroups() {
        when(policyGroupRepository.existsByAppIdAndGroupNameIgnoreCase(anyString(), anyString())).thenReturn(false);
        when(policyGroupRepository.save(any(SecurityPolicyGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        SecurityPermission p1 = new SecurityPermission("trade:so:create", "trade", "orders", "d1", "create", true);
        when(permissionRepository.findAll()).thenReturn(List.of(p1));
        when(featureService.getAllEnabled("tenant-abc")).thenReturn(Set.of("sales.enabled", "procurement.enabled", "finance.enabled", "payroll.enabled", "quality.enabled"));

        TenantVerticalResponse response = service.configureTenantVertical(BusinessVertical.MEDICAL, "admin");

        assertThat(response.vertical()).isEqualTo(BusinessVertical.MEDICAL);
        verify(featureRepository, atLeastOnce()).save(any(TenantFeature.class));
        verify(policyGroupRepository, atLeast(1)).save(any(SecurityPolicyGroup.class));
        verify(auditService).record(eq("BUSINESS_VERTICAL_CONFIGURED"), any(), any(), eq("admin"), any(), any());
    }

    @Test
    void configureTenantVertical_civil_enablesCivilFeatures() {
        when(policyGroupRepository.existsByAppIdAndGroupNameIgnoreCase(anyString(), anyString())).thenReturn(false);
        when(policyGroupRepository.save(any(SecurityPolicyGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        when(featureService.getAllEnabled("tenant-abc")).thenReturn(Set.of("procurement.enabled", "finance.enabled", "payroll.enabled", "workforce.contractorAccounts.enabled", "quality.enabled"));

        TenantVerticalResponse response = service.configureTenantVertical(BusinessVertical.CIVIL, "admin");

        assertThat(response.vertical()).isEqualTo(BusinessVertical.CIVIL);
        verify(featureRepository, atLeastOnce()).save(any(TenantFeature.class));
    }
}
