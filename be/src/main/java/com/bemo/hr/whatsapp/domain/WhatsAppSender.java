package com.bemo.hr.whatsapp.domain;

public interface WhatsAppSender {
    /** Send a template message. Returns provider message ID. */
    String sendTemplate(String phoneNumber, String templateName, String languageCode, Object... params);
}
