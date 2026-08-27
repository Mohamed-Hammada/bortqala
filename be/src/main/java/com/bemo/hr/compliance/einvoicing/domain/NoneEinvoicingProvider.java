package com.bemo.hr.compliance.einvoicing.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * NONE provider: short-circuits all operations (e-invoicing disabled for this tenant).
 */
@Component
public class NoneEinvoicingProvider implements EinvoicingProvider {

    @Override
    public EinvoicingProviderType type() {
        return EinvoicingProviderType.NONE;
    }

    @Override
    public EinvoicingResult normalize(Map<String, Object> invoicePayload) {
        throw new BusinessRuleException("E-invoicing is not configured", "EINVOICING_DISABLED", HttpStatus.BAD_REQUEST);
    }

    @Override
    public EinvoicingResult submit(String documentId) {
        throw new BusinessRuleException("E-invoicing is not configured", "EINVOICING_DISABLED", HttpStatus.BAD_REQUEST);
    }

    @Override
    public EinvoicingResult status(String externalId) {
        throw new BusinessRuleException("E-invoicing is not configured", "EINVOICING_DISABLED", HttpStatus.BAD_REQUEST);
    }
}
