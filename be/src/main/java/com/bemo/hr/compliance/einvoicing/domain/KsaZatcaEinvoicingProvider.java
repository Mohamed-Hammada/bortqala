package com.bemo.hr.compliance.einvoicing.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * KSA ZATCA Phase-2 adapter: explicit NOT_IMPLEMENTED placeholder.
 * Future: implement ZATCA clearance/reporting API integration.
 */
@Component
public class KsaZatcaEinvoicingProvider implements EinvoicingProvider {

    @Override
    public EinvoicingProviderType type() {
        return EinvoicingProviderType.KSA_ZATCA;
    }

    @Override
    public EinvoicingResult normalize(Map<String, Object> invoicePayload) {
        throw new BusinessRuleException("ZATCA e-invoicing is not yet implemented", "ZATCA_NOT_IMPLEMENTED", HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public EinvoicingResult submit(String documentId) {
        throw new BusinessRuleException("ZATCA e-invoicing is not yet implemented", "ZATCA_NOT_IMPLEMENTED", HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public EinvoicingResult status(String externalId) {
        throw new BusinessRuleException("ZATCA e-invoicing is not yet implemented", "ZATCA_NOT_IMPLEMENTED", HttpStatus.NOT_IMPLEMENTED);
    }
}
