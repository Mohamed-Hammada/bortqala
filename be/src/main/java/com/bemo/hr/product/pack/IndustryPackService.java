package com.bemo.hr.product.pack;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.product.onboarding.IndustryReadinessService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryPackService {
    private final IndustryPackRepository packRepository;
    private final TenantIndustryPackRepository tenantPackRepository;
    private final IndustryOnboardingStepRepository stepRepository;
    private final IndustryPackReconciliationService reconciliationService;
    private final IndustryReadinessService readinessService;
    private final IndustryRoleProvisioningService roleService;
    private final IndustryImportTemplateRegistry templateRegistry;
    private final IndustryKpiRegistry kpiRegistry;
    private final IndustryPackSettingsValidator settingsValidator;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    private static BusinessRuleException error(String code, HttpStatus status) {
        return new BusinessRuleException(code, code, status);
    }

    @Transactional(readOnly = true)
    public List<IndustryPackApi.PackResponse> catalog() {
        log.debug("catalog called");
        Map<String, TenantIndustryPack> installed = tenantPackRepository.findAll().stream()
                .collect(Collectors.toMap(TenantIndustryPack::getPackId, Function.identity(), (a, b) -> a));
        return packRepository.findAllByStatusOrderByCode("ACTIVE").stream()
                .map(p -> response(p, installed.get(p.getId())))
                .toList();
    }

    @Transactional
    public IndustryPackApi.PackResponse install(String code, IndustryPackApi.InstallRequest request, String actor) {
        log.debug("install called with code={}, operationId={}", code, request.operationId());
        TenantContext.require();
        Optional<TenantIndustryPack> replay = tenantPackRepository.findByOperationId(request.operationId());
        if (replay.isPresent()) {
            IndustryPack p = packRepository.findById(replay.get().getPackId())
                    .orElseThrow(() -> error("INDUSTRY_PACK_NOT_FOUND", HttpStatus.NOT_FOUND));
            return response(p, replay.get());
        }
        IndustryPack pack = require(code);
        reconciliationService.validatePack(pack);

        Optional<TenantIndustryPack> existing = tenantPackRepository.findByPackId(pack.getId());
        if (existing.isPresent()) {
            reconciliationService.reconcile(pack, existing.get(), actor);
            return response(pack, existing.get());
        }

        reconciliationService.reconcileEntitlements(pack, actor);
        TenantIndustryPack installed = tenantPackRepository.save(new TenantIndustryPack(pack, request.operationId(), actor, defaults(pack)));
        reconciliationService.reconcileSteps(pack, installed);

        auditService.record("INSTALL", "INDUSTRY_PACK", installed.getId(), actor,
                "{\"code\":\"" + code + "\",\"version\":" + pack.getPackVersion() + "}", null);

        registerCommitLog(code, "installed", installed.getId());
        return response(pack, installed);
    }

    @Transactional
    public IndustryPackApi.PackResponse updateSettings(String code, IndustryPackApi.SettingsRequest request, String actor) {
        log.debug("updateSettings called with code={}", code);
        IndustryPack pack = require(code);
        TenantIndustryPack installed = tenantPackRepository.findByPackId(pack.getId())
                .orElseThrow(() -> error("INDUSTRY_PACK_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (installed.getVersion() != request.expectedVersion()) throw error("STALE_STATE", HttpStatus.CONFLICT);

        settingsValidator.validateSettings(pack.getCode(), request.settingsJson());

        installed.customize(request.settingsJson());
        tenantPackRepository.save(installed);
        auditService.record("CUSTOMIZE", "INDUSTRY_PACK", installed.getId(), actor,
                "{\"version\":" + installed.getInstalledVersion() + "}", null);
        registerCommitLog(code, "settings updated", installed.getId());
        return response(pack, installed);
    }

    @Transactional
    public IndustryPackApi.PackResponse upgrade(String code, IndustryPackApi.UpgradeRequest request, String actor) {
        log.debug("upgrade called with code={}, operationId={}", code, request.operationId());
        IndustryPack pack = require(code);
        TenantIndustryPack installed = tenantPackRepository.findByPackId(pack.getId())
                .orElseThrow(() -> error("INDUSTRY_PACK_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (request.operationId().equals(installed.getLastUpgradeOperationId()) || installed.getInstalledVersion() >= pack.getPackVersion()) {
            return response(pack, installed);
        }
        if (installed.getVersion() != request.expectedVersion()) throw error("STALE_STATE", HttpStatus.CONFLICT);

        reconciliationService.reconcile(pack, installed, actor);
        installed.upgrade(pack, defaults(pack), request.operationId());
        tenantPackRepository.save(installed);
        auditService.record("UPGRADE", "INDUSTRY_PACK", installed.getId(), actor,
                "{\"version\":" + pack.getPackVersion() + ",\"customized\":" + installed.isCustomized() + "}", null);
        registerCommitLog(code, "upgraded to version " + pack.getPackVersion(), installed.getId());
        return response(pack, installed);
    }

    @Transactional
    public IndustryPackApi.PackResponse reconcile(String code, IndustryPackApi.ReconcileRequest request, String actor) {
        log.debug("reconcile called with code={}", code);
        IndustryPack pack = require(code);
        TenantIndustryPack installed = tenantPackRepository.findByPackId(pack.getId())
                .orElseThrow(() -> error("INDUSTRY_PACK_NOT_INSTALLED", HttpStatus.CONFLICT));

        reconciliationService.reconcile(pack, installed, actor);
        tenantPackRepository.save(installed);
        auditService.record("RECONCILE", "INDUSTRY_PACK", installed.getId(), actor,
                "{\"reason\":\"" + (request != null && request.reason() != null ? request.reason() : "admin reconcile") + "\"}", null);
        registerCommitLog(code, "reconciled", installed.getId());
        return response(pack, installed);
    }

    @Transactional
    public IndustryPackApi.PackResponse completeStep(String code, String key, IndustryPackApi.StepRequest request, String actor) {
        log.debug("completeStep called with code={}, key={}, skip={}", code, key, request.skip());
        IndustryPack pack = require(code);
        TenantIndustryPack installed = tenantPackRepository.findByPackId(pack.getId())
                .orElseThrow(() -> error("INDUSTRY_PACK_NOT_FOUND", HttpStatus.NOT_FOUND));
        IndustryOnboardingStep step = stepRepository.findByTenantPackIdAndStepKey(installed.getId(), key)
                .orElseThrow(() -> error("INDUSTRY_PACK_STEP_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (step.getVersion() != request.expectedVersion()) throw error("STALE_STATE", HttpStatus.CONFLICT);
        try {
            step.complete(actor, request.skip());
        } catch (IllegalArgumentException ex) {
            throw error("INDUSTRY_PACK_STEP_NOT_OPTIONAL", HttpStatus.CONFLICT);
        } catch (IllegalStateException ex) {
            throw error("INDUSTRY_PACK_STEP_BLOCKED", HttpStatus.CONFLICT);
        }
        stepRepository.save(step);
        stepRepository.findByTenantPackIdOrderBySequenceNo(installed.getId()).stream()
                .filter(s -> key.equals(s.getPrerequisiteKey()))
                .forEach(IndustryOnboardingStep::ready);
        auditService.record("COMPLETE_STEP", "INDUSTRY_PACK", installed.getId(), actor,
                "{\"step\":\"" + key + "\",\"skipped\":" + request.skip() + "}", null);
        registerCommitLog(code, "step " + key + " completed", installed.getId());
        return response(pack, installed);
    }

    @Transactional(readOnly = true)
    public List<IndustryKpiProvider.KpiResult> calculateKpis(String code) {
        log.debug("calculateKpis called with code={}", code);
        IndustryPack pack = require(code);
        return kpiRegistry.calculate(array(pack.getKpisJson()));
    }

    private void registerCommitLog(String code, String action, String id) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("IndustryPack {} {} successfully, id={}", code, action, id);
                }
            });
        } else {
            log.info("IndustryPack {} {} successfully, id={}", code, action, id);
        }
    }

    private IndustryPackApi.PackResponse response(IndustryPack p, TenantIndustryPack installed) {
        List<IndustryOnboardingStep> rawSteps = installed == null
                ? List.of()
                : stepRepository.findByTenantPackIdOrderBySequenceNo(installed.getId());

        List<IndustryPackApi.StepResponse> steps = rawSteps.stream()
                .map(s -> new IndustryPackApi.StepResponse(s.getId(), s.getStepKey(), s.getSequenceNo(), s.getPrerequisiteKey(), s.isOptional(), s.getStatus().name(), s.getVersion()))
                .toList();

        boolean ready = readinessService.isGoLiveReady(p.getCode(), installed, rawSteps);

        List<IndustryPackApi.RoleReadinessResponse> roleReadiness = roleService.evaluateRoleStatus(array(p.getDefaultRolesJson()))
                .stream()
                .map(r -> new IndustryPackApi.RoleReadinessResponse(r.code(), r.required(), r.available(), r.assignedUsers(), r.status()))
                .toList();

        List<IndustryPackApi.TemplateDescriptorResponse> templateBindings = templateRegistry.resolveTemplates(array(p.getImportTemplatesJson()))
                .stream()
                .map(t -> new IndustryPackApi.TemplateDescriptorResponse(t.key(), t.fileName(), t.workflow(), t.downloadable(), t.route()))
                .toList();

        return new IndustryPackApi.PackResponse(
                p.getCode(),
                p.getNameKey(),
                p.getDescriptionKey(),
                p.getPackVersion(),
                installed == null ? null : installed.getInstalledVersion(),
                installed != null && installed.getInstalledVersion() < p.getPackVersion(),
                installed == null ? "AVAILABLE" : installed.getStatus(),
                required(p),
                array(p.getDefaultRolesJson()),
                array(p.getKpisJson()),
                array(p.getImportTemplatesJson()),
                installed == null ? null : installed.getSettingsJson(),
                installed != null && installed.isCustomized(),
                ready,
                installed == null ? 0 : installed.getVersion(),
                steps,
                roleReadiness,
                templateBindings
        );
    }

    private IndustryPack require(String code) {
        return packRepository.findByCodeAndStatus(code, "ACTIVE")
                .orElseThrow(() -> error("INDUSTRY_PACK_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private List<String> required(IndustryPack p) {
        return array(p.getRequiredFeaturesJson());
    }

    private List<String> array(String json) {
        try {
            return json == null ? List.of() : List.of(objectMapper.readValue(json, String[].class));
        } catch (Exception ex) {
            throw error("INDUSTRY_PACK_SETTINGS_INVALID", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String defaults(IndustryPack p) {
        try {
            objectMapper.readTree(p.getDefaultsJson());
            return p.getDefaultsJson();
        } catch (Exception ex) {
            throw error("INDUSTRY_PACK_SETTINGS_INVALID", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
