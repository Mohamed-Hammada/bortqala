package com.bemo.hr.compliance.einvoicing.domain;

import com.bemo.hr.compliance.eta.application.EtaComplianceService;
import com.bemo.hr.compliance.eta.domain.EtaDocumentType;
import com.bemo.hr.compliance.eta.api.EtaComplianceApi;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Egypt ETA adapter: wraps existing EtaComplianceService logic.
 */
@Component
public class EtaEinvoicingProvider implements EinvoicingProvider {

    private final EtaComplianceService etaService;

    public EtaEinvoicingProvider(EtaComplianceService etaService) {
        this.etaService = etaService;
    }

    @Override
    public EinvoicingProviderType type() {
        return EinvoicingProviderType.EGYPT_ETA;
    }

    @Override
    public EinvoicingResult normalize(Map<String, Object> invoicePayload) {
        String invoiceId = (String) invoicePayload.get("invoiceId");
        String docType = (String) invoicePayload.getOrDefault("documentType", "INVOICE");
        EtaComplianceApi.QueueInvoiceRequest request = new EtaComplianceApi.QueueInvoiceRequest(
                invoiceId, EtaDocumentType.valueOf(docType));
        EtaComplianceApi.SubmissionResponse response = etaService.queueInvoice(request);
        return new EinvoicingResult(response.id(), "QUEUED", response.canonicalJsonHash(), true);
    }

    @Override
    public EinvoicingResult submit(String documentId) {
        EtaComplianceApi.SubmissionResponse response = etaService.submitToEta(documentId);
        return new EinvoicingResult(response.etaUuid(), response.status().name(), response.rawResponseJson(), true);
    }

    @Override
    public EinvoicingResult status(String externalId) {
        throw new com.bemo.hr.shared.domain.BusinessRuleException(
                "Status check not implemented for ETA provider", "EINVOICING_NOT_IMPLEMENTED", org.springframework.http.HttpStatus.NOT_IMPLEMENTED);
    }
}
