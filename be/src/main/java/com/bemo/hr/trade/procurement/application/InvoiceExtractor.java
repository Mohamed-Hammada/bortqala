package com.bemo.hr.trade.procurement.application;

/**
 * Pluggable OCR extraction interface. V1 adapter returns a mock/none result;
 * real providers (OpenAI Vision, custom) plug in via configuration.
 */
public interface InvoiceExtractor {

    /** Returns true when the provider is configured and available. */
    boolean isConfigured();

    /** Provider name for display (e.g. "NONE", "OPENAI_VISION", "CUSTOM_URL"). */
    String providerName();

    /**
     * Extract invoice fields from the image bytes.
     * Returns a JSON string with structure: {supplierName?, invoiceNo?, date?, lines[{name,qty,unitPrice}]}
     */
    String extract(byte[] imageBytes, String contentType);

    /** Returns overall confidence summary string (e.g. "0.82"). */
    default String confidenceSummary() { return "0.00"; }
}
