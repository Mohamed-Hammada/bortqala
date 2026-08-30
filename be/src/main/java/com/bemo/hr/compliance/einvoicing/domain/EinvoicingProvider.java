package com.bemo.hr.compliance.einvoicing.domain;

import java.util.Map;

/**
 * Country-provider abstraction for multi-country e-invoicing.
 * Each implementation handles normalize → submit → status for a specific country's regulation.
 */
public interface EinvoicingProvider {

    /** Returns the provider type this adapter handles. */
    EinvoicingProviderType type();

    /** Normalize an invoice into a country-specific document payload. */
    EinvoicingResult normalize(Map<String, Object> invoicePayload);

    /** Submit the normalized document to the government gateway. */
    EinvoicingResult submit(String documentId);

    /** Check submission status. */
    EinvoicingResult status(String externalId);

    record EinvoicingResult(String externalId, String status, String rawResponse, boolean success) {
    }
}
