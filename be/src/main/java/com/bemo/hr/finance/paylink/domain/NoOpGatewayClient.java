package com.bemo.hr.finance.paylink.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * Stub gateway used when hr.payments.gateway=NONE (feature off).
 * All operations throw — callers must check the gateway type before invoking.
 */
public class NoOpGatewayClient implements PaymentGatewayClient {

    @Override
    public String createCheckout(String token, BigDecimal amount, String currency, String description) {
        throw new BusinessRuleException("Payment gateways are not configured.",
                "PAYLINK_GATEWAY_OFF", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public WebhookResult verifyWebhook(String body, String signatureHeader) {
        throw new BusinessRuleException("Payment gateways are not configured.",
                "PAYLINK_GATEWAY_OFF", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
