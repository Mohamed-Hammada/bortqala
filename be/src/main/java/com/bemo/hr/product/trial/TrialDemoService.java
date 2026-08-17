package com.bemo.hr.product.trial;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrialDemoService {
    private final TenantApplicationRepository tenantRepository;
    private final DemoTenantTemplateRepository templateRepository;
    private final DemoSampleRecordRepository sampleRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public TrialDemoApi.StatusResponse status() {
        return view(current());
    }

    @Transactional(readOnly = true)
    public List<TrialDemoApi.TemplateResponse> templates() {
        return templateRepository.findAllByActiveTrueOrderByCodeAscTemplateVersionDesc().stream()
                .map(t -> new TrialDemoApi.TemplateResponse(t.getCode(), t.getTemplateVersion(), t.getNameKey())).toList();
    }

    @Transactional
    public TrialDemoApi.StatusResponse start(TrialDemoApi.StartRequest request, String actor) {
        TenantApplication app = current();
        Instant now = Instant.now();
        DemoTenantTemplate template = null;
        if (request.operationId().equals(app.getLastTrialOperationId())) return view(app);
        if (request.demo()) template = template(request.templateCode(), request.templateVersion());
        app.startTrial(now, now.plus(request.days(), java.time.temporal.ChronoUnit.DAYS), request.demo(),
                template == null ? null : template.getCode(), template == null ? null : template.getTemplateVersion(), request.operationId());
        tenantRepository.save(app);
        auditService.record("START_TRIAL", "TENANT_APPLICATION", app.getId(), actor,
                "{\"days\":" + request.days() + ",\"demo\":" + request.demo() + ",\"operationId\":\"" + request.operationId() + "\"}", null);
        if (template != null) resetOwnedSamples(app, template, request.operationId() + "-seed", actor, now);
        return view(app);
    }

    @Transactional
    public TrialDemoApi.StatusResponse convert(TrialDemoApi.ConvertRequest request, String actor) {
        TenantApplication app = current();
        if (request.operationId().equals(app.getLastConversionOperationId())) return view(app);
        String id = app.getId();
        app.convertTrial(Instant.now(), request.operationId());
        tenantRepository.save(app);
        auditService.record("CONVERT_TRIAL", "TENANT_APPLICATION", id, actor,
                "{\"operationId\":\"" + request.operationId() + "\",\"tenantPreserved\":true}", null);
        return view(app);
    }

    @Transactional
    public TrialDemoApi.StatusResponse reset(TrialDemoApi.ResetRequest request, String actor) {
        TenantApplication app = current();
        if (!app.isDemoTenant()) throw error("DEMO_RESET_NOT_ALLOWED", HttpStatus.CONFLICT);
        if (request.operationId().equals(app.getLastDemoResetOperationId())) return view(app);
        DemoTenantTemplate template = template(app.getDemoTemplateCode(), request.templateVersion());
        resetOwnedSamples(app, template, request.operationId(), actor, Instant.now());
        return view(app);
    }

    @Transactional(readOnly = true)
    public void assertWriteAllowed() {
        TenantApplication app = current();
        if (app.isTrialExpired(Instant.now())) throw error("TRIAL_EXPIRED_READ_ONLY", HttpStatus.PAYMENT_REQUIRED);
    }

    private void resetOwnedSamples(TenantApplication app, DemoTenantTemplate template, String operationId, String actor, Instant now) {
        sampleRepository.deleteOwnedByTenant(app.getId());
        try {
            JsonNode rows = objectMapper.readTree(template.getSampleJson());
            for (JsonNode row : rows)
                sampleRepository.save(new DemoSampleRecord(template.getCode(), template.getTemplateVersion(),
                        row.path("key").asText(), row.path("payload").toString(), operationId, now));
        } catch (Exception ex) {
            throw error("DEMO_TEMPLATE_INVALID", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        app.recordDemoReset(operationId, actor, now, template.getCode(), template.getTemplateVersion());
        tenantRepository.save(app);
        auditService.record("RESET_DEMO", "TENANT_APPLICATION", app.getId(), actor,
                "{\"template\":\"" + template.getCode() + "\",\"version\":" + template.getTemplateVersion() + ",\"operationId\":\"" + operationId + "\"}", null);
    }

    private DemoTenantTemplate template(String code, Integer version) {
        if (code == null || code.isBlank()) throw error("DEMO_TEMPLATE_REQUIRED", HttpStatus.BAD_REQUEST);
        return (version == null ? templateRepository.findFirstByCodeAndActiveTrueOrderByTemplateVersionDesc(code)
                : templateRepository.findByCodeAndTemplateVersionAndActiveTrue(code, version))
                .orElseThrow(() -> error("DEMO_TEMPLATE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private TenantApplication current() {
        return tenantRepository.findById(TenantContext.require()).orElseThrow(() -> error("TENANT_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private TrialDemoApi.StatusResponse view(TenantApplication app) {
        Instant now = Instant.now();
        String state = app.isTrialExpired(now) ? "EXPIRED" : app.getCommercialState();
        List<TrialDemoApi.SampleResponse> samples = app.isDemoTenant() ? sampleRepository.findAllByOrderByRecordKey().stream()
                                                                         .map(r -> new TrialDemoApi.SampleResponse(r.getRecordKey(), r.getPayloadJson())).toList() : List.of();
        return new TrialDemoApi.StatusResponse(app.getId(), state, epoch(app.getTrialStartedAt()), epoch(app.getTrialEndsAt()), epoch(app.getConvertedAt()),
                !"EXPIRED".equals(state), app.isDemoTenant(), app.getDemoTemplateCode(), app.getDemoTemplateVersion(),
                epoch(app.getLastDemoResetAt()), app.getLastDemoResetBy(), samples.size(), samples);
    }

    private long epoch(Instant value) {
        return value == null ? 0 : value.toEpochMilli();
    }

    private BusinessRuleException error(String code, HttpStatus status) {
        return new BusinessRuleException(code, code, status);
    }
}
