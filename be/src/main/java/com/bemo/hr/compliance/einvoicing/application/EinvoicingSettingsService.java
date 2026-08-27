package com.bemo.hr.compliance.einvoicing.application;

import com.bemo.hr.compliance.einvoicing.api.EinvoicingApi;
import com.bemo.hr.compliance.einvoicing.domain.*;
import com.bemo.hr.compliance.einvoicing.infrastructure.EinvoicingSettingsRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class EinvoicingSettingsService {

    private final EinvoicingSettingsRepository settingsRepository;
    private final List<EinvoicingProvider> providers;

    public EinvoicingSettingsService(EinvoicingSettingsRepository settingsRepository,
                                     List<EinvoicingProvider> providers) {
        this.settingsRepository = settingsRepository;
        this.providers = providers;
    }

    @Transactional(readOnly = true)
    public Optional<EinvoicingApi.SettingsResponse> getSettings() {
        String appId = TenantContext.require();
        return settingsRepository.findFirstByAppIdOrderByUpdatedAtDesc(appId)
                .map(this::toResponse);
    }

    @Transactional
    public EinvoicingApi.SettingsResponse saveSettings(EinvoicingApi.SaveSettingsRequest request) {
        String appId = TenantContext.require();
        EinvoicingSettings settings = settingsRepository.findFirstByAppIdOrderByUpdatedAtDesc(appId).orElse(null);
        if (settings == null) {
            settings = new EinvoicingSettings(request.provider(), request.environment());
        } else {
            settings.update(request.provider(), request.environment());
        }
        EinvoicingSettings saved = settingsRepository.save(settings);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EinvoicingApi.ProviderInfo> listProviders() {
        return Stream.of(
                new EinvoicingApi.ProviderInfo(EinvoicingProviderType.EGYPT_ETA, "compliance.provider.egyptEta", true),
                new EinvoicingApi.ProviderInfo(EinvoicingProviderType.KSA_ZATCA, "compliance.provider.ksaZatca", false),
                new EinvoicingApi.ProviderInfo(EinvoicingProviderType.NONE, "compliance.provider.none", true)
        ).toList();
    }

    private EinvoicingApi.SettingsResponse toResponse(EinvoicingSettings s) {
        return new EinvoicingApi.SettingsResponse(
                s.getId(), s.getProvider(), s.getEnvironment(),
                s.getCreatedAt(), s.getUpdatedAt());
    }
}
