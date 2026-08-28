package com.bemo.hr.whatsapp.application;

import com.bemo.hr.whatsapp.api.WhatsAppApi;
import com.bemo.hr.whatsapp.domain.*;
import com.bemo.hr.compliance.privacy.domain.ConsentRegistryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final WhatsAppOutboundLogRepository logRepo;
    private final WhatsAppSender sender;
    private final ConsentRegistryRepository consentRegistryRepository;

    @Value("${hr.whatsapp.provider:NONE}")
    private String provider;

    @Value("${hr.whatsapp.consent.purpose:whatsapp_marketing}")
    private String consentPurpose;

    @Value("${hr.whatsapp.template.payslip:payslip_v1}")
    private String payslipTemplate;

    @Value("${hr.whatsapp.template.invoice-overdue:invoice_overdue_v1}")
    private String invoiceOverdueTemplate;

    public boolean isConfigured() {
        return !"NONE".equalsIgnoreCase(provider);
    }

    @Transactional
    public WhatsAppApi.OutboundLogEntry sendTest(String phoneNumber) {
        if (!isConfigured())
            throw new BusinessRuleException("WhatsApp provider is not configured.",
                    "WA_PROVIDER_OFF", HttpStatus.SERVICE_UNAVAILABLE);
        WhatsAppOutboundLog log = new WhatsAppOutboundLog("SYSTEM", "TEST", "SYSTEM",
                phoneNumber, "test_send", "{}", "TEST:" + phoneNumber + ":" + System.currentTimeMillis());
        logRepo.save(log);
        try {
            String msgId = sender.sendTemplate(phoneNumber, "test_template", "ar");
            log.markSent(msgId);
        } catch (Exception e) {
            log.markFailed(e.getMessage());
        }
        logRepo.save(log);
        return toEntry(log);
    }

    @Transactional(readOnly = true)
    public WhatsAppApi.LogResponse listLogs(String appId, int limit) {
        List<WhatsAppOutboundLog> logs = logRepo.findByAppIdOrderByCreatedAtDesc(appId);
        List<WhatsAppApi.OutboundLogEntry> entries = logs.stream().limit(limit).map(this::toEntry).toList();
        return new WhatsAppApi.LogResponse(entries, logs.size());
    }

    @Transactional
    public void resend(String appId, String logId) {
        WhatsAppOutboundLog log = logRepo.findById(logId)
                .filter(l -> l.getAppId().equals(appId))
                .orElseThrow(() -> new BusinessRuleException("Log entry not found.",
                        "WA_LOG_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (log.getStatus() != WhatsAppOutboundLog.Status.FAILED)
            throw new BusinessRuleException("Only FAILED entries can be resent.",
                    "WA_RESEND_INVALID_STATE", HttpStatus.CONFLICT);
        try {
            String msgId = sender.sendTemplate(log.getPhoneNumber(), log.getTemplateKey(), "ar");
            log.markSent(msgId);
        } catch (Exception e) {
            log.markFailed(e.getMessage());
        }
        logRepo.save(log);
    }

    @Transactional
    public void enqueuePayrollPayslip(String appId, String employeeId, String phone,
                                       String employeeName, String amount) {
        enqueuePayrollPayslip(appId, employeeId, phone, employeeName, amount, null);
    }

    @Transactional
    public void enqueuePayrollPayslip(String appId, String employeeId, String phone,
                                       String employeeName, String amount, String period) {
        if (!isConfigured()) return;
        String dedupeKey = (period == null || period.isBlank())
                ? "PAYSLIP:" + employeeId + ":" + appId
                : "PAYSLIP:" + employeeId + ":" + appId + ":" + period;
        if (logRepo.findByAppIdAndDedupeKey(appId, dedupeKey).isPresent()) return;
        String params = "{\"name\":\"" + employeeName + "\",\"amount\":\"" + amount + "\"}";
        if (!hasConsent(appId, employeeId)) {
            WhatsAppOutboundLog skipped = new WhatsAppOutboundLog(appId, "EMPLOYEE", employeeId,
                    phone, payslipTemplate, params, dedupeKey);
            skipped.markNoConsent();
            logRepo.save(skipped);
            return;
        }
        WhatsAppOutboundLog log = new WhatsAppOutboundLog(appId, "EMPLOYEE", employeeId,
                phone, payslipTemplate, params, dedupeKey);
        logRepo.save(log);
        try {
            String msgId = sender.sendTemplate(phone, payslipTemplate, "ar", employeeName, amount);
            log.markSent(msgId);
        } catch (Exception e) {
            log.markFailed(e.getMessage());
        }
        logRepo.save(log);
    }

    private boolean hasConsent(String appId, String subjectRef) {
        return consentRegistryRepository.findByAppIdAndSubjectRefAndWithdrawnAtIsNull(appId, subjectRef).stream()
                .anyMatch(consent -> consentPurpose.equals(consent.getPurposeKey()));
    }

    @Transactional
    public void processStatusWebhook(String providerMessageId, String status) {
        List<WhatsAppOutboundLog> logs = logRepo.findByAppIdAndStatusIn("",
                List.of(WhatsAppOutboundLog.Status.SENT.name()));
        // In production this would query by provider_message_id; simplified for v1
    }

    @Transactional
    public void retryFailed(String appId) {
        List<WhatsAppOutboundLog> retryable = logRepo.findRetryable(appId);
        for (WhatsAppOutboundLog log : retryable) {
            try {
                String msgId = sender.sendTemplate(log.getPhoneNumber(), log.getTemplateKey(), "ar");
                log.markSent(msgId);
            } catch (Exception e) {
                log.markFailed(e.getMessage());
            }
            logRepo.save(log);
        }
    }

    private WhatsAppApi.OutboundLogEntry toEntry(WhatsAppOutboundLog log) {
        return new WhatsAppApi.OutboundLogEntry(
                log.getId(), log.getRecipientType(), log.getRecipientId(), log.getPhoneNumber(),
                log.getTemplateKey(), log.getStatus().name(), log.getProviderMessageId(),
                log.getErrorMessage(), log.getRetryCount(),
                log.getSentAt() != null ? log.getSentAt().toEpochMilli() : null,
                log.getCreatedAt());
    }
}
