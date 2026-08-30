package com.bemo.hr.finance.paylink.domain;

import java.math.BigDecimal;

public interface PaymentGatewayClient {
    /**
     * Initialize a gateway checkout session for the given payment link.
     * Returns the gateway reference ID to store on the link.
     */
    String createCheckout(String token, BigDecimal amount, String currency, String description);

    /**
     * Verify and parse a webhook payload. Returns the provider transaction ID,
     * the confirmed amount, and a JSON string of the raw payload.
     * Throws if signature is invalid.
     */
    WebhookResult verifyWebhook(String body, String signatureHeader);

    record WebhookResult(String providerTxnId, BigDecimal amount, String rawPayload) {}
}
