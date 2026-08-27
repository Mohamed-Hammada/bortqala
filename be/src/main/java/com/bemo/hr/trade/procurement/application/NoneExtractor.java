package com.bemo.hr.trade.procurement.application;

import org.springframework.stereotype.Component;

/**
 * V1 extractor: provider not configured. Safe to ship — returns explicit error code.
 */
@Component
public class NoneExtractor implements InvoiceExtractor {

    @Override
    public boolean isConfigured() { return false; }

    @Override
    public String providerName() { return "NONE"; }

    @Override
    public String extract(byte[] imageBytes, String contentType) {
        throw new UnsupportedOperationException("OCR provider is not configured.");
    }
}
