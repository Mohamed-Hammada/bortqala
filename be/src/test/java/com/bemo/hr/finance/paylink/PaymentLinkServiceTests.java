package com.bemo.hr.finance.paylink;

import com.bemo.hr.finance.paylink.api.PaylinkApi;
import com.bemo.hr.finance.paylink.application.PaymentLinkService;
import com.bemo.hr.finance.paylink.domain.*;
import com.bemo.hr.notification.BusinessNotificationRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentLinkServiceTests {

    @Mock PaymentLinkRepository linkRepo;
    @Mock GatewayTransactionRepository txnRepo;
    @Mock PaymentGatewayClient gatewayClient;
    @Mock BusinessNotificationRepository notificationRepo;

    PaymentLinkService service;

    @BeforeEach
    void setUp() {
        service = new PaymentLinkService(linkRepo, txnRepo, gatewayClient, notificationRepo);
        ReflectionTestUtils.setField(service, "gatewayType", "PAYMOB");
        ReflectionTestUtils.setField(service, "linkTtlHours", 48L);
    }

    @Test
    void createLink_persistsAndReturnsResponse() {
        when(linkRepo.save(any(PaymentLink.class))).thenAnswer(inv -> inv.getArgument(0));
        var payload = new PaylinkApi.CreateLinkPayload("INVOICE", "ref-1", BigDecimal.valueOf(5000), "Invoice #1", null);
        var result = service.createLink("app-1", payload, "Bemo ERP");
        assertNotNull(result.id());
        assertEquals("INVOICE", result.kind());
        assertEquals(BigDecimal.valueOf(5000), result.amount());
        assertEquals("EGP", result.currency());
        assertEquals("PENDING", result.status());
        verify(linkRepo).save(any(PaymentLink.class));
    }

    @Test
    void createLink_gatewayOff_throws() {
        ReflectionTestUtils.setField(service, "gatewayType", "NONE");
        var payload = new PaylinkApi.CreateLinkPayload("INVOICE", "ref-1", BigDecimal.valueOf(100), "Test", null);
        var ex = assertThrows(BusinessRuleException.class,
                () -> service.createLink("app-1", payload, "Bemo ERP"));
        assertEquals("PAYLINK_GATEWAY_OFF", ex.getCode());
    }

    @Test
    void cancelLink_pending_succeeds() {
        PaymentLink link = new PaymentLink("app-1", PaymentLink.Kind.INVOICE, "ref-1",
                BigDecimal.valueOf(100), "desc", "Bemo", Instant.now().plusSeconds(86400));
        when(linkRepo.findById(link.getId())).thenReturn(Optional.of(link));
        when(linkRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.cancelLink("app-1", link.getId());
        assertEquals(PaymentLink.Status.CANCELLED, link.getStatus());
    }

    @Test
    void handleWebhook_expiredLink_throws() {
        PaymentLink link = new PaymentLink("app-1", PaymentLink.Kind.INVOICE, "ref-1",
                BigDecimal.valueOf(100), "desc", "Bemo", Instant.now().minusSeconds(3600));
        when(linkRepo.findByToken("tok")).thenReturn(Optional.of(link));
        var payload = new PaylinkApi.WebhookPayload("txn-1", "sig");
        var ex = assertThrows(BusinessRuleException.class, () -> service.handleWebhook("tok", payload));
        assertEquals("PAYLINK_EXPIRED", ex.getCode());
    }

    @Test
    void handleWebhook_idempotent_duplicateTxn_noop() {
        PaymentLink link = new PaymentLink("app-1", PaymentLink.Kind.INVOICE, "ref-1",
                BigDecimal.valueOf(100), "desc", "Bemo", Instant.now().plusSeconds(86400));
        when(linkRepo.findByToken("tok")).thenReturn(Optional.of(link));
        var result = new PaymentGatewayClient.WebhookResult("txn-1", BigDecimal.valueOf(100), "{}");
        when(gatewayClient.verifyWebhook("txn-1", "sig")).thenReturn(result);
        when(txnRepo.findByAppIdAndProviderTxnId("app-1", "txn-1")).thenReturn(Optional.of(
                new GatewayTransaction("app-1", "link-1", "txn-1", "{}", java.math.BigDecimal.valueOf(100), java.time.Instant.now())
        ));
        service.handleWebhook("tok", new PaylinkApi.WebhookPayload("txn-1", "sig"));
        assertEquals(PaymentLink.Status.PENDING, link.getStatus());
        verify(linkRepo, never()).save(any());
    }

    @Test
    void getPublicPage_notFound_throws() {
        when(linkRepo.findByToken("bad")).thenReturn(Optional.empty());
        assertThrows(BusinessRuleException.class, () -> service.getPublicPage("bad"));
    }
}
