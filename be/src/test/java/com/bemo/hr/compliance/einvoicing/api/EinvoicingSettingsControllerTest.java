package com.bemo.hr.compliance.einvoicing.api;

import com.bemo.hr.compliance.einvoicing.application.EinvoicingSettingsService;
import com.bemo.hr.compliance.einvoicing.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EinvoicingSettingsControllerTest {

    @Mock
    private EinvoicingSettingsService service;

    @InjectMocks
    private EinvoicingSettingsController controller;

    @Test
    void getSettingsReturns204_whenNoContent() {
        when(service.getSettings()).thenReturn(Optional.empty());

        ResponseEntity<EinvoicingApi.SettingsResponse> response = controller.getSettings();

        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void getSettingsReturns200_whenSettingsExist() {
        EinvoicingApi.SettingsResponse resp = new EinvoicingApi.SettingsResponse(
                "id1", EinvoicingProviderType.EGYPT_ETA, EinvoicingEnvironment.TEST,
                System.currentTimeMillis(), System.currentTimeMillis());
        when(service.getSettings()).thenReturn(Optional.of(resp));

        ResponseEntity<EinvoicingApi.SettingsResponse> response = controller.getSettings();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().provider()).isEqualTo(EinvoicingProviderType.EGYPT_ETA);
    }

    @Test
    void saveSettingsReturns200() {
        EinvoicingApi.SaveSettingsRequest req = new EinvoicingApi.SaveSettingsRequest(
                EinvoicingProviderType.NONE, EinvoicingEnvironment.TEST);
        EinvoicingApi.SettingsResponse resp = new EinvoicingApi.SettingsResponse(
                "id1", EinvoicingProviderType.NONE, EinvoicingEnvironment.TEST,
                System.currentTimeMillis(), System.currentTimeMillis());
        when(service.saveSettings(req)).thenReturn(resp);

        ResponseEntity<EinvoicingApi.SettingsResponse> response = controller.saveSettings(req);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().provider()).isEqualTo(EinvoicingProviderType.NONE);
    }

    @Test
    void listProvidersReturnsAll() {
        List<EinvoicingApi.ProviderInfo> providers = List.of(
                new EinvoicingApi.ProviderInfo(EinvoicingProviderType.EGYPT_ETA, "eta", true),
                new EinvoicingApi.ProviderInfo(EinvoicingProviderType.KSA_ZATCA, "zatca", false));
        when(service.listProviders()).thenReturn(providers);

        ResponseEntity<List<EinvoicingApi.ProviderInfo>> response = controller.listProviders();

        assertThat(response.getBody()).hasSize(2);
    }
}
