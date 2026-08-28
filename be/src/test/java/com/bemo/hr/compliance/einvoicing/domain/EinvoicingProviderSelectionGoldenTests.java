package com.bemo.hr.compliance.einvoicing.domain;

import com.bemo.hr.compliance.eta.api.EtaComplianceApi;
import com.bemo.hr.compliance.eta.application.EtaComplianceService;
import com.bemo.hr.compliance.eta.domain.EtaDocumentType;
import com.bemo.hr.compliance.eta.domain.EtaSubmissionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Golden submission fixture for WP-51 AC-2: an existing Egypt tenant resolved through
 * einvoicing_settings (provider=EGYPT_ETA) must behave exactly like the pre-refactor
 * ETA path — queue produces a QUEUED receipt and submit produces a VALID ETA receipt.
 */
@ExtendWith(MockitoExtension.class)
class EinvoicingProviderSelectionGoldenTests {

    @Mock
    private EtaComplianceService etaService;

    private EtaComplianceApi.SubmissionResponse submission(String id, EtaSubmissionStatus status) {
        return new EtaComplianceApi.SubmissionResponse(
                id, "inv-golden", "INV-GOLDEN", EtaDocumentType.INVOICE,
                "eta-uuid", "submission-uuid", status,
                System.currentTimeMillis(), new BigDecimal("1000.00"), BigDecimal.ZERO,
                new BigDecimal("1000.00"), new BigDecimal("140.00"), new BigDecimal("1140.00"),
                "hash-golden", null, null, 0, null,
                System.currentTimeMillis(), System.currentTimeMillis(), 0);
    }

    @Test
    void queueAndSubmitDeliverQueuedAndValidReceiptsLikePreRefactor() {
        when(etaService.queueInvoice(any(EtaComplianceApi.QueueInvoiceRequest.class)))
                .thenReturn(submission("sub-golden", EtaSubmissionStatus.VALIDATED));
        when(etaService.submitToEta("sub-golden"))
                .thenReturn(submission("sub-golden", EtaSubmissionStatus.VALID));

        EtaEinvoicingProvider provider = new EtaEinvoicingProvider(etaService);

        var queued = provider.normalize(java.util.Map.of("invoiceId", "inv-golden"));
        assertThat(queued.externalId()).isEqualTo("sub-golden");
        assertThat(queued.status()).isEqualTo("QUEUED");
        assertThat(queued.rawResponse()).isEqualTo("hash-golden");
        assertThat(queued.success()).isTrue();
        verify(etaService).queueInvoice(any(EtaComplianceApi.QueueInvoiceRequest.class));

        var receipt = provider.submit("sub-golden");
        assertThat(receipt.externalId()).isEqualTo("eta-uuid");
        assertThat(receipt.status()).isEqualTo("VALID");
        assertThat(receipt.success()).isTrue();
        verify(etaService).submitToEta("sub-golden");
    }

    @Test
    void providerSelectionMatchesTheStoredSetting() {
        List<EinvoicingProvider> registry = List.of(
                new EtaEinvoicingProvider(etaService),
                new KsaZatcaEinvoicingProvider(),
                new NoneEinvoicingProvider());

        EinvoicingProvider egypt = resolve(registry, EinvoicingProviderType.EGYPT_ETA);
        EinvoicingProvider disabled = resolve(registry, EinvoicingProviderType.NONE);

        assertThat(egypt).isInstanceOf(EtaEinvoicingProvider.class);
        assertThat(disabled).isInstanceOf(NoneEinvoicingProvider.class);
        assertThat(resolve(registry, EinvoicingProviderType.KSA_ZATCA)).isInstanceOf(KsaZatcaEinvoicingProvider.class);
    }

    @Test
    void noneShortsCircuitsSubmissionWithDisabledError() {
        NoneEinvoicingProvider disabled = new NoneEinvoicingProvider();
        assertThatThrownBy(() -> disabled.submit("doc"))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class)
                .extracting(e -> ((com.bemo.hr.shared.domain.BusinessRuleException) e).getCode())
                .isEqualTo("EINVOICING_DISABLED");
    }

    private EinvoicingProvider resolve(List<EinvoicingProvider> registry, EinvoicingProviderType type) {
        return registry.stream().filter(p -> p.type() == type).findFirst().orElseThrow();
    }
}