package com.bemo.hr.product.pack;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.EntitlementApi;
import com.bemo.hr.shared.security.EntitlementManagementService;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryPackReconciliationService {
    private final IndustryOnboardingStepRepository stepRepository;
    private final EntitlementManagementService entitlementService;
    private final IndustryRoleProvisioningService roleService;
    private final IndustryKpiRegistry kpiRegistry;
    private final IndustryImportTemplateRegistry templateRegistry;
    private final IndustryPackSettingsValidator settingsValidator;
    private final ObjectMapper objectMapper;

    public void validatePack(IndustryPack pack) {
        roleService.validateRoles(array(pack.getDefaultRolesJson()));
        kpiRegistry.validateKpiKeys(array(pack.getKpisJson()));
        templateRegistry.validateTemplates(array(pack.getImportTemplatesJson()));
        settingsValidator.validateSettings(pack.getCode(), pack.getDefaultsJson());
    }

    @Transactional
    public void reconcile(IndustryPack pack, TenantIndustryPack installed, String actor) {
        log.info("Reconciling industry pack {} for tenant pack id={}", pack.getCode(), installed.getId());
        validatePack(pack);
        reconcileEntitlements(pack, actor);
        reconcileSteps(pack, installed);
        if (!installed.isCustomized()) {
            installed.upgrade(pack, pack.getDefaultsJson(), installed.getLastUpgradeOperationId());
        }
    }

    @Transactional
    public void reconcileEntitlements(IndustryPack pack, String actor) {
        String app = TenantContext.require();
        List<String> requiredFeatures = array(pack.getRequiredFeaturesJson());
        var catalog = entitlementService.catalog(app);
        for (String feature : requiredFeatures) {
            var currentOpt = catalog.stream()
                    .flatMap(m -> m.features().stream())
                    .filter(f -> f.key().equals(feature))
                    .findFirst();
            if (currentOpt.isEmpty()) {
                log.warn("Entitlement feature not found in catalog: {}", feature);
                throw new BusinessRuleException("ENTITLEMENT_UNKNOWN_FEATURE", "ENTITLEMENT_UNKNOWN_FEATURE", HttpStatus.CONFLICT);
            }
            var current = currentOpt.get();
            if (!current.enabled()) {
                entitlementService.update(app, feature, new EntitlementApi.UpdateRequest(true, null, "Industry pack " + pack.getCode(), current.version()), actor);
            }
        }
    }

    @Transactional
    public void reconcileSteps(IndustryPack pack, TenantIndustryPack installed) {
        List<String> declaredStepKeys = array(pack.getOnboardingStepsJson());
        Map<String, IndustryOnboardingStep> existingSteps = stepRepository.findByTenantPackIdOrderBySequenceNo(installed.getId())
                .stream()
                .collect(Collectors.toMap(IndustryOnboardingStep::getStepKey, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        String previousKey = null;
        for (int i = 0; i < declaredStepKeys.size(); i++) {
            String raw = declaredStepKeys.get(i);
            boolean optional = raw.endsWith("?");
            String key = optional ? raw.substring(0, raw.length() - 1) : raw;

            IndustryOnboardingStep step = existingSteps.get(key);
            if (step == null) {
                step = new IndustryOnboardingStep(installed.getId(), key, i + 1, previousKey, optional);
                if (previousKey == null || isTerminal(existingSteps.get(previousKey))) {
                    step.ready();
                }
            } else {
                if (step.getStatus() == IndustryOnboardingStep.Status.BLOCKED) {
                    if (previousKey == null || isTerminal(existingSteps.get(previousKey))) {
                        step.ready();
                    }
                }
            }
            stepRepository.save(step);
            existingSteps.put(key, step);
            previousKey = key;
        }
    }

    private boolean isTerminal(IndustryOnboardingStep step) {
        return step != null && (step.getStatus() == IndustryOnboardingStep.Status.COMPLETED || step.getStatus() == IndustryOnboardingStep.Status.SKIPPED);
    }

    private List<String> array(String json) {
        try {
            return json == null ? List.of() : List.of(objectMapper.readValue(json, String[].class));
        } catch (Exception ex) {
            log.error("Failed to parse JSON array: {}", json, ex);
            throw new BusinessRuleException("INDUSTRY_PACK_SETTINGS_INVALID", "INDUSTRY_PACK_SETTINGS_INVALID", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
