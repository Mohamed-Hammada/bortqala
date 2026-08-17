package com.bemo.hr.product.trial;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrialDemoServiceTests {
    @Mock
    TenantApplicationRepository tenantRepository;
    @Mock
    DemoTenantTemplateRepository templateRepository;
    @Mock
    DemoSampleRecordRepository sampleRepository;
    @Mock
    AuditService auditService;
    TrialDemoService service;
    TenantApplication tenant;
    DemoTenantTemplate template;
    List<DemoSampleRecord> samples;

    @BeforeEach
    void setup() {
        tenant = new TenantApplication("trial", "Trial Tenant");
        TenantContext.set(tenant.getId());
        template = new DemoTenantTemplate("tpl", "CONTRACTOR_WORKFORCE_EG", 1, "[{\"key\":\"worker\",\"payload\":{\"code\":\"DEMO-1\"}}]");
        samples = new ArrayList<>();
        service = new TrialDemoService(tenantRepository, templateRepository, sampleRepository, new ObjectMapper(), auditService);
        lenient().when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        lenient().when(tenantRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(templateRepository.findFirstByCodeAndActiveTrueOrderByTemplateVersionDesc("CONTRACTOR_WORKFORCE_EG")).thenReturn(Optional.of(template));
        lenient().when(templateRepository.findByCodeAndTemplateVersionAndActiveTrue("CONTRACTOR_WORKFORCE_EG", 1)).thenReturn(Optional.of(template));
        lenient().when(sampleRepository.save(any())).thenAnswer(i -> {
            DemoSampleRecord row = i.getArgument(0);
            samples.add(row);
            return row;
        });
        lenient().when(sampleRepository.findAllByOrderByRecordKey()).thenAnswer(i -> List.copyOf(samples));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void startsFourteenDayDemoAndSeedsVersionedSamples() {
        var result = service.start(new TrialDemoApi.StartRequest(14, true, "CONTRACTOR_WORKFORCE_EG", null, "start-1"), "admin");
        assertThat(result.commercialState()).isEqualTo("TRIAL");
        assertThat(result.trialEndsAt() - result.trialStartedAt()).isEqualTo(14L * 24 * 60 * 60 * 1000);
        assertThat(result.demoTenant()).isTrue();
        assertThat(result.templateVersion()).isEqualTo(1);
        assertThat(result.sampleCount()).isEqualTo(1);
    }

    @Test
    void expiredTrialRejectsWritesButKeepsReadsAvailable() {
        tenant.startTrial(Instant.now().minusSeconds(100), Instant.now().minusSeconds(1), false, null, null, "expired");
        assertThat(service.status().commercialState()).isEqualTo("EXPIRED");
        assertThatThrownBy(service::assertWriteAllowed).isInstanceOfSatisfying(BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("TRIAL_EXPIRED_READ_ONLY"));
    }

    @Test
    void conversionPreservesTenantAndSampleBusinessState() {
        service.start(new TrialDemoApi.StartRequest(14, true, "CONTRACTOR_WORKFORCE_EG", 1, "start"), "admin");
        String tenantId = tenant.getId();
        var result = service.convert(new TrialDemoApi.ConvertRequest("convert"), "admin");
        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.commercialState()).isEqualTo("PAID");
        assertThat(result.sampleCount()).isEqualTo(1);
    }

    @Test
    void nonDemoTenantCannotReset() {
        assertThatThrownBy(() -> service.reset(new TrialDemoApi.ResetRequest("reset", null), "admin")).isInstanceOfSatisfying(BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("DEMO_RESET_NOT_ALLOWED"));
    }

    @Test
    void resetIsReplaySafeAndAuditsTemplateVersion() {
        service.start(new TrialDemoApi.StartRequest(14, true, "CONTRACTOR_WORKFORCE_EG", 1, "start"), "admin");
        samples.clear();
        var first = service.reset(new TrialDemoApi.ResetRequest("reset-1", 1), "admin");
        var replay = service.reset(new TrialDemoApi.ResetRequest("reset-1", 1), "admin");
        assertThat(first.sampleCount()).isEqualTo(1);
        assertThat(replay.sampleCount()).isEqualTo(1);
        verify(sampleRepository, times(2)).deleteOwnedByTenant(tenant.getId());
        verify(auditService, times(2)).record(eq("RESET_DEMO"), eq("TENANT_APPLICATION"), eq(tenant.getId()), eq("admin"), contains("\"version\":1"), isNull());
    }

    @Test
    void startReplayDoesNotExtendTrialOrReseed() {
        var request = new TrialDemoApi.StartRequest(14, true, "CONTRACTOR_WORKFORCE_EG", 1, "same-start");
        var first = service.start(request, "admin");
        var replay = service.start(request, "admin");
        assertThat(replay.trialEndsAt()).isEqualTo(first.trialEndsAt());
        verify(sampleRepository, times(1)).deleteOwnedByTenant(tenant.getId());
        verify(auditService, times(1)).record(eq("START_TRIAL"), any(), any(), any(), any(), isNull());
    }

    @Test
    void conversionReplayIsSideEffectFree() {
        service.start(new TrialDemoApi.StartRequest(14, false, null, null, "start"), "admin");
        var request = new TrialDemoApi.ConvertRequest("same-conversion");
        service.convert(request, "admin");
        service.convert(request, "admin");
        verify(auditService, times(1)).record(eq("CONVERT_TRIAL"), any(), any(), any(), any(), isNull());
    }
}
