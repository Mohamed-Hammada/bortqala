package com.bemo.hr.product.pack;

import com.bemo.hr.audit.application.AuditService;
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
    private final EntitlementManagementService entitlementService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    private static BusinessRuleException error(String code, HttpStatus status) {
        return new BusinessRuleException(code, code, status);
    }

    @Transactional(readOnly = true)
    public List<IndustryPackApi.PackResponse> catalog() {
        log.debug("catalog called");
        Map<String, TenantIndustryPack> installed = tenantPackRepository.findAll().stream().collect(Collectors.toMap(TenantIndustryPack::getPackId, Function.identity()));
        return packRepository.findAllByStatusOrderByCode("ACTIVE").stream().map(p -> response(p, installed.get(p.getId()))).toList();
    }

    @Transactional
    public IndustryPackApi.PackResponse install(String code, IndustryPackApi.InstallRequest request, String actor) {
        log.debug("install called with code={}, operationId={}", code, request.operationId());
        String app = TenantContext.require();
        Optional<TenantIndustryPack> replay = tenantPackRepository.findByOperationId(request.operationId());
        if (replay.isPresent()) {
            IndustryPack p = packRepository.findById(replay.get().getPackId()).orElseThrow(() -> error("INDUSTRY_PACK_NOT_FOUND", HttpStatus.NOT_FOUND));
            return response(p, replay.get());
        }
        IndustryPack pack = require(code);
        Optional<TenantIndustryPack> existing = tenantPackRepository.findByPackId(pack.getId());
        if (existing.isPresent()) return response(pack, existing.get());
        for (String feature : required(pack)) {
            var current = entitlementService.catalog(app).stream().flatMap(m -> m.features().stream()).filter(f -> f.key().equals(feature)).findFirst().orElseThrow(() -> error("ENTITLEMENT_UNKNOWN_FEATURE", HttpStatus.CONFLICT));
            if (!current.enabled())
                entitlementService.update(app, feature, new EntitlementApi.UpdateRequest(true, null, "Industry pack " + code, current.version()), actor);
        }
        TenantIndustryPack installed = tenantPackRepository.save(new TenantIndustryPack(pack, request.operationId(), actor, defaults(pack)));
        seedSteps(pack, installed);
        auditService.record("INSTALL", "INDUSTRY_PACK", installed.getId(), actor, "{\"code\":\"" + code + "\",\"version\":" + pack.getPackVersion() + "}", null);
        log.info("IndustryPack {} installed successfully, id={}", code, installed.getId());
        return response(pack, installed);
    }

    @Transactional
    public IndustryPackApi.PackResponse updateSettings(String code, IndustryPackApi.SettingsRequest request, String actor) {
        log.debug("updateSettings called with code={}", code);
        IndustryPack pack = require(code);
        TenantIndustryPack installed = tenantPackRepository.findByPackId(pack.getId()).orElseThrow(() -> error("INDUSTRY_PACK_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (installed.getVersion() != request.expectedVersion()) throw error("STALE_STATE", HttpStatus.CONFLICT);
        try {
            objectMapper.readTree(request.settingsJson());
        } catch (Exception ex) {
            log.warn("Validation failed: industry pack settings JSON is invalid for code={}", code);
            throw error("INDUSTRY_PACK_SETTINGS_INVALID", HttpStatus.BAD_REQUEST);
        }
        installed.customize(request.settingsJson());
        tenantPackRepository.save(installed);
        auditService.record("CUSTOMIZE", "INDUSTRY_PACK", installed.getId(), actor, "{\"version\":" + installed.getInstalledVersion() + "}", null);
        log.info("IndustryPack {} settings updated, id={}", code, installed.getId());
        return response(pack, installed);
    }

    @Transactional
    public IndustryPackApi.PackResponse upgrade(String code, IndustryPackApi.UpgradeRequest request, String actor) {
        log.debug("upgrade called with code={}, operationId={}", code, request.operationId());
        IndustryPack pack = require(code);
        TenantIndustryPack installed = tenantPackRepository.findByPackId(pack.getId()).orElseThrow(() -> error("INDUSTRY_PACK_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (request.operationId().equals(installed.getLastUpgradeOperationId()) || installed.getInstalledVersion() >= pack.getPackVersion())
            return response(pack, installed);
        if (installed.getVersion() != request.expectedVersion()) throw error("STALE_STATE", HttpStatus.CONFLICT);
        installed.upgrade(pack, defaults(pack), request.operationId());
        tenantPackRepository.save(installed);
        auditService.record("UPGRADE", "INDUSTRY_PACK", installed.getId(), actor, "{\"version\":" + pack.getPackVersion() + ",\"customized\":" + installed.isCustomized() + "}", null);
        log.info("IndustryPack {} upgraded to version {}, id={}", code, pack.getPackVersion(), installed.getId());
        return response(pack, installed);
    }

    @Transactional
    public IndustryPackApi.PackResponse completeStep(String code, String key, IndustryPackApi.StepRequest request, String actor) {
        log.debug("completeStep called with code={}, key={}, skip={}", code, key, request.skip());
        IndustryPack pack = require(code);
        TenantIndustryPack installed = tenantPackRepository.findByPackId(pack.getId()).orElseThrow(() -> error("INDUSTRY_PACK_NOT_FOUND", HttpStatus.NOT_FOUND));
        IndustryOnboardingStep step = stepRepository.findByTenantPackIdAndStepKey(installed.getId(), key).orElseThrow(() -> error("INDUSTRY_PACK_STEP_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (step.getVersion() != request.expectedVersion()) throw error("STALE_STATE", HttpStatus.CONFLICT);
        try {
            step.complete(actor, request.skip());
        } catch (IllegalArgumentException ex) {
            throw error("INDUSTRY_PACK_STEP_NOT_OPTIONAL", HttpStatus.CONFLICT);
        } catch (IllegalStateException ex) {
            throw error("INDUSTRY_PACK_STEP_BLOCKED", HttpStatus.CONFLICT);
        }
        stepRepository.save(step);
        stepRepository.findByTenantPackIdOrderBySequenceNo(installed.getId()).stream().filter(s -> key.equals(s.getPrerequisiteKey())).forEach(IndustryOnboardingStep::ready);
        auditService.record("COMPLETE_STEP", "INDUSTRY_PACK", installed.getId(), actor, "{\"step\":\"" + key + "\",\"skipped\":" + request.skip() + "}", null);
        log.info("IndustryPack {} step {} completed (skip={}), packId={}", code, key, request.skip(), installed.getId());
        return response(pack, installed);
    }

    private void seedSteps(IndustryPack pack, TenantIndustryPack p) {
        String previous = null;
        List<String> keys = array(pack.getOnboardingStepsJson());
        for (int i = 0; i < keys.size(); i++) {
            String raw = keys.get(i);
            boolean optional = raw.endsWith("?");
            String key = optional ? raw.substring(0, raw.length() - 1) : raw;
            stepRepository.save(new IndustryOnboardingStep(p.getId(), key, i + 1, previous, optional));
            previous = key;
        }
    }

    private IndustryPackApi.PackResponse response(IndustryPack p, TenantIndustryPack installed) {
        List<IndustryPackApi.StepResponse> steps = installed == null ? List.of() : stepRepository.findByTenantPackIdOrderBySequenceNo(installed.getId()).stream().map(s -> new IndustryPackApi.StepResponse(s.getId(), s.getStepKey(), s.getSequenceNo(), s.getPrerequisiteKey(), s.isOptional(), s.getStatus().name(), s.getVersion())).toList();
        boolean ready = installed != null && steps.stream().filter(s -> !s.optional()).allMatch(s -> s.status().equals("COMPLETED"));
        return new IndustryPackApi.PackResponse(p.getCode(), p.getNameKey(), p.getDescriptionKey(), p.getPackVersion(), installed == null ? null : installed.getInstalledVersion(), installed != null && installed.getInstalledVersion() < p.getPackVersion(), installed == null ? "AVAILABLE" : installed.getStatus(), required(p), array(p.getDefaultRolesJson()), array(p.getKpisJson()), array(p.getImportTemplatesJson()), installed == null ? null : installed.getSettingsJson(), installed != null && installed.isCustomized(), ready, installed == null ? 0 : installed.getVersion(), steps);
    }

    private IndustryPack require(String code) {
        return packRepository.findByCodeAndStatus(code, "ACTIVE").orElseThrow(() -> error("INDUSTRY_PACK_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private List<String> required(IndustryPack p) {
        return array(p.getRequiredFeaturesJson());
    }

    private List<String> array(String json) {
        try {
            return List.of(objectMapper.readValue(json, String[].class));
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
