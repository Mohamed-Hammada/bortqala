package com.bemo.hr.whatsapp;

import com.bemo.hr.whatsapp.application.WhatsAppService;
import com.bemo.hr.whatsapp.domain.*;
import com.bemo.hr.compliance.privacy.domain.ConsentRegistry;
import com.bemo.hr.compliance.privacy.domain.ConsentRegistryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class WhatsAppServiceTests {

    @Mock WhatsAppOutboundLogRepository logRepo;
    @Mock WhatsAppSender sender;
    @Mock ConsentRegistryRepository consentRegistryRepository;

    WhatsAppService service;

    @BeforeEach
    void setUp() {
        service = new WhatsAppService(logRepo, sender, consentRegistryRepository);
        ReflectionTestUtils.setField(service, "provider", "CLOUD_API");
        ReflectionTestUtils.setField(service, "consentPurpose", "whatsapp_marketing");
        ReflectionTestUtils.setField(service, "payslipTemplate", "payslip_v1");
        ReflectionTestUtils.setField(service, "invoiceOverdueTemplate", "invoice_overdue_v1");
    }

    @Test
    void isConfigured_cloudApi_returnsTrue() {
        assertTrue(service.isConfigured());
    }

    @Test
    void isConfigured_none_returnsFalse() {
        ReflectionTestUtils.setField(service, "provider", "NONE");
        assertFalse(service.isConfigured());
    }

    @Test
    void sendTest_success_marksSent() {
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sender.sendTemplate("+20123", "test_template", "ar")).thenReturn("msg-123");
        var result = service.sendTest("+20123");
        assertEquals("SENT", result.status());
        assertEquals("msg-123", result.providerMessageId());
    }

    @Test
    void sendTest_failure_marksFailed() {
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sender.sendTemplate(any(), any(), any())).thenThrow(new RuntimeException("timeout"));
        var result = service.sendTest("+20123");
        assertEquals("FAILED", result.status());
        assertEquals("timeout", result.errorMessage());
    }

    @Test
    void enqueuePayrollPayslip_noConsent_skipsSend() {
        ReflectionTestUtils.setField(service, "provider", "NONE");
        // Should not throw or call sender
        service.enqueuePayrollPayslip("app-1", "emp-1", "+20123", "Ahmed", "5000");
        verify(sender, never()).sendTemplate(any(), any(), any());
    }

    @Test
    void enqueuePayrollPayslip_dedupes() {
        WhatsAppOutboundLog existing = new WhatsAppOutboundLog("app-1", "EMPLOYEE", "emp-1",
                "+20123", "payslip_v1", "{}", "PAYSLIP:emp-1:app-1");
        when(logRepo.findByAppIdAndDedupeKey("app-1", "PAYSLIP:emp-1:app-1"))
                .thenReturn(Optional.of(existing));
        service.enqueuePayrollPayslip("app-1", "emp-1", "+20123", "Ahmed", "5000");
        verify(sender, never()).sendTemplate(any(), any(), any());
        verify(logRepo, never()).save(any());
    }

    @Test
    void enqueuePayrollPayslip_withPeriod_usesPeriodScopedDedupeKey() {
        WhatsAppOutboundLog existing = new WhatsAppOutboundLog("app-1", "EMPLOYEE", "emp-1",
                "+20123", "payslip_v1", "{}", "PAYSLIP:emp-1:app-1:2026-08");
        when(logRepo.findByAppIdAndDedupeKey("app-1", "PAYSLIP:emp-1:app-1:2026-08"))
                .thenReturn(Optional.of(existing));
        service.enqueuePayrollPayslip("app-1", "emp-1", "+20123", "Ahmed", "5000", "2026-08");
        verify(logRepo).findByAppIdAndDedupeKey("app-1", "PAYSLIP:emp-1:app-1:2026-08");
        verify(sender, never()).sendTemplate(any(), any(), any());
    }

    @Test
    void enqueuePayrollPayslip_withoutConsent_marksNoConsentAndNeverSends() {
        when(logRepo.findByAppIdAndDedupeKey(any(), any())).thenReturn(Optional.empty());
        when(consentRegistryRepository.findByAppIdAndSubjectRefAndWithdrawnAtIsNull("app-1", "emp-1"))
                .thenReturn(List.of());
        when(logRepo.save(any(WhatsAppOutboundLog.class))).thenAnswer(inv -> inv.getArgument(0));

        service.enqueuePayrollPayslip("app-1", "emp-1", "+20123", "Ahmed", "5000");

        var captured = ArgumentCaptor.forClass(WhatsAppOutboundLog.class);
        verify(logRepo).save(captured.capture());
        assertEquals(WhatsAppOutboundLog.Status.NO_CONSENT, captured.getValue().getStatus());
        verify(sender, never()).sendTemplate(any(), any(), any());
    }

    @Test
    void enqueuePayrollPayslip_withActiveConsent_sends() {
        when(logRepo.findByAppIdAndDedupeKey(any(), any())).thenReturn(Optional.empty());
        when(consentRegistryRepository.findByAppIdAndSubjectRefAndWithdrawnAtIsNull("app-1", "emp-1"))
                .thenReturn(List.of(new ConsentRegistry("app-1", "emp-1", "EMPLOYEE", "whatsapp_marketing")));
        when(logRepo.save(any(WhatsAppOutboundLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sender.sendTemplate("+20123", "payslip_v1", "ar", "Ahmed", "5000")).thenReturn("msg-1");

        service.enqueuePayrollPayslip("app-1", "emp-1", "+20123", "Ahmed", "5000");

        var captured = ArgumentCaptor.forClass(WhatsAppOutboundLog.class);
        verify(logRepo, times(2)).save(captured.capture());
        assertEquals(WhatsAppOutboundLog.Status.SENT, captured.getAllValues().get(1).getStatus());
        verify(sender).sendTemplate("+20123", "payslip_v1", "ar", "Ahmed", "5000");
    }

    @Test
    void enqueuePayrollPayslip_withdrawnConsent_doesNotSend() {
        ConsentRegistry withdrawn = new ConsentRegistry("app-1", "emp-1", "EMPLOYEE", "whatsapp_marketing");
        withdrawn.withdraw();
        when(logRepo.findByAppIdAndDedupeKey(any(), any())).thenReturn(Optional.empty());
        when(consentRegistryRepository.findByAppIdAndSubjectRefAndWithdrawnAtIsNull("app-1", "emp-1"))
                .thenReturn(List.of());
        when(logRepo.save(any(WhatsAppOutboundLog.class))).thenAnswer(inv -> inv.getArgument(0));

        service.enqueuePayrollPayslip("app-1", "emp-1", "+20123", "Ahmed", "5000");

        var captured = ArgumentCaptor.forClass(WhatsAppOutboundLog.class);
        verify(logRepo).save(captured.capture());
        assertEquals(WhatsAppOutboundLog.Status.NO_CONSENT, captured.getValue().getStatus());
        verify(sender, never()).sendTemplate(any(), any(), any());
    }

    @Test
    void resend_onlyFailedEntries() {
        WhatsAppOutboundLog log = new WhatsAppOutboundLog("app-1", "EMPLOYEE", "emp-1",
                "+20123", "payslip_v1", "{}", "dedupe");
        log.markSent("msg-1");
        when(logRepo.findById("log-1")).thenReturn(Optional.of(log));
        assertThrows(BusinessRuleException.class, () -> service.resend("app-1", "log-1"));
    }
}
