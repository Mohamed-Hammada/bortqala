package com.bemo.hr.compliance.einvoicing.application;

import com.bemo.hr.compliance.einvoicing.api.EinvoicingApi;
import com.bemo.hr.compliance.einvoicing.domain.*;
import com.bemo.hr.compliance.einvoicing.infrastructure.EinvoicingSettingsRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EinvoicingSettingsServiceTest {

    @Mock
    private EinvoicingSettingsRepository settingsRepository;

    @Mock
    private java.util.List<EinvoicingProvider> providers;

    @InjectMocks
    private EinvoicingSettingsService service;

    private MockedStatic<TenantContext> tenantCtx;

    @BeforeEach
    void setUp() {
        tenantCtx = mockStatic(TenantContext.class);
        tenantCtx.when(TenantContext::require).thenReturn("test-app");
    }

    @AfterEach
    void tearDown() {
        tenantCtx.close();
    }

    @Test
    void getSettingsReturnsEmpty_whenNoSettingsExist() {
        when(settingsRepository.findFirstByAppIdOrderByUpdatedAtDesc("test-app"))
                .thenReturn(Optional.empty());

        Optional<EinvoicingApi.SettingsResponse> result = service.getSettings();

        assertThat(result).isEmpty();
    }

    @Test
    void getSettingsReturnsResponse_whenSettingsExist() {
        EinvoicingSettings settings = new EinvoicingSettings(EinvoicingProviderType.EGYPT_ETA, EinvoicingEnvironment.TEST);
        when(settingsRepository.findFirstByAppIdOrderByUpdatedAtDesc("test-app"))
                .thenReturn(Optional.of(settings));

        Optional<EinvoicingApi.SettingsResponse> result = service.getSettings();

        assertThat(result).isPresent();
        assertThat(result.get().provider()).isEqualTo(EinvoicingProviderType.EGYPT_ETA);
        assertThat(result.get().environment()).isEqualTo(EinvoicingEnvironment.TEST);
    }

    @Test
    void saveSettingsCreatesNew_whenNoneExist() {
        when(settingsRepository.findFirstByAppIdOrderByUpdatedAtDesc("test-app"))
                .thenReturn(Optional.empty());
        when(settingsRepository.save(any(EinvoicingSettings.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        EinvoicingApi.SaveSettingsRequest req = new EinvoicingApi.SaveSettingsRequest(
                EinvoicingProviderType.KSA_ZATCA, EinvoicingEnvironment.PRODUCTION);
        EinvoicingApi.SettingsResponse result = service.saveSettings(req);

        assertThat(result.provider()).isEqualTo(EinvoicingProviderType.KSA_ZATCA);
        assertThat(result.environment()).isEqualTo(EinvoicingEnvironment.PRODUCTION);
    }

    @Test
    void saveSettingsUpdatesExisting_whenSettingsExist() {
        EinvoicingSettings existing = new EinvoicingSettings(EinvoicingProviderType.NONE, EinvoicingEnvironment.TEST);
        when(settingsRepository.findFirstByAppIdOrderByUpdatedAtDesc("test-app"))
                .thenReturn(Optional.of(existing));
        when(settingsRepository.save(any(EinvoicingSettings.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        EinvoicingApi.SaveSettingsRequest req = new EinvoicingApi.SaveSettingsRequest(
                EinvoicingProviderType.EGYPT_ETA, EinvoicingEnvironment.PRODUCTION);
        EinvoicingApi.SettingsResponse result = service.saveSettings(req);

        assertThat(result.provider()).isEqualTo(EinvoicingProviderType.EGYPT_ETA);
        assertThat(result.environment()).isEqualTo(EinvoicingEnvironment.PRODUCTION);
    }

    @Test
    void listProvidersReturnsAllThree() {
        var result = service.listProviders();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).type()).isEqualTo(EinvoicingProviderType.EGYPT_ETA);
        assertThat(result.get(2).type()).isEqualTo(EinvoicingProviderType.NONE);
    }

    private EinvoicingProvider mockProvider(EinvoicingProviderType type) {
        EinvoicingProvider p = mock(EinvoicingProvider.class);
        when(p.type()).thenReturn(type);
        return p;
    }
}
