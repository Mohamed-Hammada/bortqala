package com.bemo.hr.whatsapp.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;

/**
 * Stub sender when hr.whatsapp.provider=NONE. Zero external calls.
 */
public class NoOpWhatsAppSender implements WhatsAppSender {
    @Override
    public String sendTemplate(String phoneNumber, String templateName, String languageCode, Object... params) {
        throw new BusinessRuleException("WhatsApp provider is not configured.",
                "WA_PROVIDER_OFF", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
