package com.bemo.hr.product.pack;

import com.bemo.hr.shared.security.EntitlementApi;
import com.bemo.hr.shared.security.EntitlementManagementService;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndustryPackReconciliationTests {
    @Mock
    IndustryOnboardingStepRepository stepRepository;
    @Mock
    EntitlementManagementService entitlementService;
    @Mock
    IndustryRoleProvisioningService roleService;
    @Mock
    IndustryKpiRegistry kpiRegistry;
    @Mock
    IndustryImportTemplateRegistry templateRegistry;
    @Mock
    IndustryPackSettingsValidator settingsValidator;

    IndustryPackReconciliationService service;
    List<IndustryOnboardingStep> storedSteps;

    @BeforeEach
    void setup() {
        TenantContext.set("app-test");
        storedSteps = new ArrayList<>();
        service = new IndustryPackReconciliationService(
                stepRepository, entitlementService, roleService, kpiRegistry, templateRegistry, settingsValidator, new ObjectMapper()
        );
        lenient().when(stepRepository.findByTenantPackIdOrderBySequenceNo(anyString())).thenAnswer(i -> storedSteps);
        lenient().when(stepRepository.save(any())).thenAnswer(i -> {
            IndustryOnboardingStep s = i.getArgument(0);
            if (!storedSteps.contains(s)) storedSteps.add(s);
            return s;
        });
        lenient().when(entitlementService.catalog("app-test")).thenReturn(List.of(
                new EntitlementApi.ModuleResponse("M", List.of(
                        new EntitlementApi.FeatureResponse("feat.one", true, List.of(), null, 0, null, null, 0),
                        new EntitlementApi.FeatureResponse("feat.two", false, List.of(), null, 0, null, null, 0)
                ))
        ));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void reconciliationEnablesNewlyRequiredFeatures() {
        IndustryPack pack = new IndustryPack("p1", "PACK_1", 1, "[\"feat.two\"]");
        TenantIndustryPack installed = new TenantIndustryPack(pack, "op", "admin", "{}");

        service.reconcile(pack, installed, "admin");

        verify(entitlementService).update(eq("app-test"), eq("feat.two"), any(), eq("admin"));
    }

    @Test
    void reconciliationAddsNewStepsWithoutOverwritingCompletedStatus() {
        IndustryPack pack = new IndustryPack("p1", "PACK_1", "p1.name", "p1.desc", 1,
                "[\"feat.one\"]", "{}", "[\"WORKFORCE_MANAGER\"]", "[\"fillRate\"]", "[\"workers.xlsx\"]",
                "[\"step.one\",\"step.two\",\"step.three\"]");
        TenantIndustryPack installed = new TenantIndustryPack(pack, "op", "admin", "{}");

        IndustryOnboardingStep existingStep1 = new IndustryOnboardingStep(installed.getId(), "step.one", 1, null, false);
        existingStep1.complete("admin", false);
        storedSteps.add(existingStep1);

        service.reconcile(pack, installed, "admin");

        assertThat(storedSteps).hasSize(3);
        assertThat(storedSteps.get(0).getStatus()).isEqualTo(IndustryOnboardingStep.Status.COMPLETED);
        assertThat(storedSteps.get(1).getStatus()).isEqualTo(IndustryOnboardingStep.Status.READY);
    }
}
