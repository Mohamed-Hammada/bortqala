package com.bemo.hr.product.pack;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.product.onboarding.IndustryReadinessService;
import com.bemo.hr.product.onboarding.OnboardingEvidenceRegistry;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.AppUserRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndustryPackServiceTests {
    @Mock
    IndustryPackRepository packRepository;
    @Mock
    TenantIndustryPackRepository tenantPackRepository;
    @Mock
    IndustryOnboardingStepRepository stepRepository;
    @Mock
    EntitlementManagementService entitlementService;
    @Mock
    AppUserRepository userRepository;
    @Mock
    AuditService auditService;

    IndustryPackService service;
    IndustryPackReconciliationService reconciliationService;
    IndustryReadinessService readinessService;
    IndustryRoleProvisioningService roleService;
    IndustryKpiRegistry kpiRegistry;
    IndustryImportTemplateRegistry templateRegistry;
    IndustryPackSettingsValidator settingsValidator;
    ObjectMapper objectMapper;
    IndustryPack pack;
    List<IndustryOnboardingStep> steps;

    @BeforeEach
    void setup() {
        TenantContext.set("app");
        pack = new IndustryPack("pack-1", "CONTRACTOR_WORKFORCE_EG", 1, "[\"workforce.enabled\"]");
        steps = new ArrayList<>();
        objectMapper = new ObjectMapper();
        roleService = new IndustryRoleProvisioningService(userRepository);
        kpiRegistry = new IndustryKpiRegistry(List.of());
        templateRegistry = new IndustryImportTemplateRegistry();
        settingsValidator = new IndustryPackSettingsValidator(objectMapper);
        reconciliationService = new IndustryPackReconciliationService(
                stepRepository, entitlementService, roleService, kpiRegistry, templateRegistry, settingsValidator, objectMapper
        );
        readinessService = new IndustryReadinessService(new OnboardingEvidenceRegistry(List.of()));

        service = new IndustryPackService(
                packRepository, tenantPackRepository, stepRepository, reconciliationService,
                readinessService, roleService, templateRegistry, settingsValidator, objectMapper, auditService
        );

        lenient().when(packRepository.findByCodeAndStatus(pack.getCode(), "ACTIVE")).thenReturn(Optional.of(pack));
        lenient().when(packRepository.findById(pack.getId())).thenReturn(Optional.of(pack));
        lenient().when(tenantPackRepository.findByOperationId(anyString())).thenReturn(Optional.empty());
        lenient().when(tenantPackRepository.findByPackId(pack.getId())).thenReturn(Optional.empty());
        lenient().when(tenantPackRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(stepRepository.save(any())).thenAnswer(i -> {
            IndustryOnboardingStep s = i.getArgument(0);
            if (!steps.contains(s)) steps.add(s);
            return s;
        });
        lenient().when(stepRepository.findByTenantPackIdOrderBySequenceNo(anyString())).thenAnswer(i -> steps);
        lenient().when(entitlementService.catalog("app")).thenReturn(List.of(
                new EntitlementApi.ModuleResponse("WORKFORCE", List.of(
                        new EntitlementApi.FeatureResponse("workforce.enabled", true, List.of(), null, 0, null, null, 0)
                ))
        ));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void installIsVersionedAndSeedsOrderedOnboarding() {
        var result = service.install(pack.getCode(), new IndustryPackApi.InstallRequest("op-1"), "admin");
        assertThat(result.installedVersion()).isEqualTo(1);
        assertThat(result.requiredFeatures()).containsExactly("workforce.enabled");
        assertThat(steps).hasSize(7);
        assertThat(steps.get(0).getStatus()).isEqualTo(IndustryOnboardingStep.Status.READY);
        assertThat(steps.get(1).getStatus()).isEqualTo(IndustryOnboardingStep.Status.BLOCKED);
        assertThat(steps.get(5).isOptional()).isTrue();
        verify(auditService).record(eq("INSTALL"), eq("INDUSTRY_PACK"), anyString(), eq("admin"), contains("CONTRACTOR_WORKFORCE_EG"), isNull());
    }

    @Test
    void operationReplayDoesNotInstallAgain() {
        TenantIndustryPack installed = new TenantIndustryPack(pack, "op-replay", "admin", "{}");
        when(tenantPackRepository.findByOperationId("op-replay")).thenReturn(Optional.of(installed));
        service.install(pack.getCode(), new IndustryPackApi.InstallRequest("op-replay"), "admin");
        verify(tenantPackRepository, never()).save(any());
    }

    @Test
    void blockedStepCannotCompleteBeforePrerequisite() {
        TenantIndustryPack installed = new TenantIndustryPack(pack, "op", "admin", "{}");
        IndustryOnboardingStep blocked = new IndustryOnboardingStep(installed.getId(), "second", 2, "first", false);
        when(tenantPackRepository.findByPackId(pack.getId())).thenReturn(Optional.of(installed));
        when(stepRepository.findByTenantPackIdAndStepKey(installed.getId(), "second")).thenReturn(Optional.of(blocked));
        assertThatThrownBy(() -> service.completeStep(pack.getCode(), "second", new IndustryPackApi.StepRequest(false, 0), "admin"))
                .isInstanceOfSatisfying(BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("INDUSTRY_PACK_STEP_BLOCKED"));
    }

    @Test
    void customizedSettingsAreValidatedAndMarked() {
        TenantIndustryPack installed = new TenantIndustryPack(pack, "op", "admin", "{}");
        when(tenantPackRepository.findByPackId(pack.getId())).thenReturn(Optional.of(installed));
        var result = service.updateSettings(pack.getCode(), new IndustryPackApi.SettingsRequest("{\"dashboard\":\"workforce\"}", 0), "admin");
        assertThat(result.customized()).isTrue();
        assertThat(result.settingsJson()).contains("workforce");
    }

    @Test
    void invalidSettingsSchemaThrowsBusinessRuleException() {
        TenantIndustryPack installed = new TenantIndustryPack(pack, "op", "admin", "{}");
        when(tenantPackRepository.findByPackId(pack.getId())).thenReturn(Optional.of(installed));
        assertThatThrownBy(() -> service.updateSettings(pack.getCode(), new IndustryPackApi.SettingsRequest("{\"dashboard\":\"invalid_val\"}", 0), "admin"))
                .isInstanceOfSatisfying(BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("INDUSTRY_PACK_SETTINGS_INVALID"));
    }

    @Test
    void upgradePreservesCustomizedSettingsAndReconciles() {
        TenantIndustryPack installed = new TenantIndustryPack(pack, "install", "admin", "{}");
        installed.customize("{\"dashboard\":\"workforce\"}");
        IndustryPack v2 = new IndustryPack(pack.getId(), pack.getCode(), 2, "[\"workforce.enabled\"]");
        when(packRepository.findByCodeAndStatus(pack.getCode(), "ACTIVE")).thenReturn(Optional.of(v2));
        when(tenantPackRepository.findByPackId(pack.getId())).thenReturn(Optional.of(installed));
        when(tenantPackRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.upgrade(pack.getCode(), new IndustryPackApi.UpgradeRequest("upgrade-1", 0), "admin");
        assertThat(result.installedVersion()).isEqualTo(2);
        assertThat(result.settingsJson()).contains("workforce");
        service.upgrade(pack.getCode(), new IndustryPackApi.UpgradeRequest("upgrade-1", 1), "admin");
        verify(tenantPackRepository, times(1)).save(installed);
    }

    @Test
    void foodDistributionUsesItsOwnMetadataAndOnboarding() {
        IndustryPack food = new IndustryPack("food", "FOOD_DISTRIBUTION_EG", "food.name", "food.description", 1,
                "[\"procurement.enabled\"]", "{\"issuePolicy\":\"FEFO\",\"creditControl\":true,\"expiryWindowsDays\":[7,30,60],\"dashboard\":\"foodDistribution\"}",
                "[\"SALES_MANAGER\",\"INVENTORY_MANAGER\"]", "[\"expiryRiskValue\",\"fillRate\"]",
                "[\"items.xlsx\",\"customers.xlsx\"]", "[\"industryPack.food.step.company\",\"industryPack.food.step.warehouses\",\"industryPack.food.step.margin?\"]");
        when(packRepository.findByCodeAndStatus(food.getCode(), "ACTIVE")).thenReturn(Optional.of(food));
        when(tenantPackRepository.findByPackId(food.getId())).thenReturn(Optional.empty());
        when(entitlementService.catalog("app")).thenReturn(List.of(
                new EntitlementApi.ModuleResponse("PROCUREMENT", List.of(
                        new EntitlementApi.FeatureResponse("procurement.enabled", true, List.of(), null, 0, null, null, 0)
                ))
        ));
        steps.clear();
        var result = service.install(food.getCode(), new IndustryPackApi.InstallRequest("food-op"), "admin");
        assertThat(result.defaultRoles()).containsExactly("SALES_MANAGER", "INVENTORY_MANAGER");
        assertThat(result.kpis()).containsExactly("expiryRiskValue", "fillRate");
        assertThat(result.importTemplates()).containsExactly("items.xlsx", "customers.xlsx");
        assertThat(result.settingsJson()).contains("FEFO");
        assertThat(steps).hasSize(3);
        assertThat(steps.get(2).isOptional()).isTrue();
    }

    @Test
    void invalidRoleThrowsBusinessRuleException() {
        IndustryPack invalid = new IndustryPack("bad", "BAD_PACK", "bad.name", "bad.desc", 1,
                "[]", "{}", "[\"NON_EXISTENT_ROLE_XYZ\"]", "[\"fillRate\"]", "[\"workers.xlsx\"]", "[\"company\"]");
        when(packRepository.findByCodeAndStatus(invalid.getCode(), "ACTIVE")).thenReturn(Optional.of(invalid));
        assertThatThrownBy(() -> service.install(invalid.getCode(), new IndustryPackApi.InstallRequest("op"), "admin"))
                .isInstanceOfSatisfying(BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("INDUSTRY_PACK_ROLE_UNKNOWN"));
    }

    @Test
    void invalidKpiThrowsBusinessRuleException() {
        IndustryPack invalid = new IndustryPack("bad", "BAD_PACK", "bad.name", "bad.desc", 1,
                "[]", "{}", "[\"WORKFORCE_MANAGER\"]", "[\"nonExistentKpi\"]", "[\"workers.xlsx\"]", "[\"company\"]");
        when(packRepository.findByCodeAndStatus(invalid.getCode(), "ACTIVE")).thenReturn(Optional.of(invalid));
        assertThatThrownBy(() -> service.install(invalid.getCode(), new IndustryPackApi.InstallRequest("op"), "admin"))
                .isInstanceOfSatisfying(BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("INDUSTRY_PACK_KPI_UNKNOWN"));
    }

    @Test
    void invalidTemplateThrowsBusinessRuleException() {
        IndustryPack invalid = new IndustryPack("bad", "BAD_PACK", "bad.name", "bad.desc", 1,
                "[]", "{}", "[\"WORKFORCE_MANAGER\"]", "[\"fillRate\"]", "[\"unknown_random_template.csv\"]", "[\"company\"]");
        when(packRepository.findByCodeAndStatus(invalid.getCode(), "ACTIVE")).thenReturn(Optional.of(invalid));
        assertThatThrownBy(() -> service.install(invalid.getCode(), new IndustryPackApi.InstallRequest("op"), "admin"))
                .isInstanceOfSatisfying(BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("INDUSTRY_PACK_TEMPLATE_UNKNOWN"));
    }
}
