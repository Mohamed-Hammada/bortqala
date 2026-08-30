package com.bemo.hr.crm.api;

import com.bemo.hr.crm.application.CrmService;
import com.bemo.hr.crm.domain.CrmChannelConfig;
import com.bemo.hr.crm.domain.CrmChannelType;
import com.bemo.hr.crm.infrastructure.CrmChannelConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WhatsAppWebhookControllerTests {

    private CrmService crmService;
    private CrmChannelConfigRepository channelConfigRepository;
    private WhatsAppWebhookController controller;
    private CrmChannelConfig whatsappConfig;

    @BeforeEach
    void setUp() {
        crmService = mock(CrmService.class);
        channelConfigRepository = mock(CrmChannelConfigRepository.class);
        controller = new WhatsAppWebhookController(crmService, channelConfigRepository);

        whatsappConfig = new CrmChannelConfig(
                CrmChannelType.WHATSAPP,
                "Official WhatsApp",
                "201012345678",
                "••••1234",
                "test-webhook-secret",
                true,
                "Welcome to Bemo!",
                true
        );
    }

    @Test
    void verifyWebhookReturnsChallengeWhenTokenMatches() {
        when(channelConfigRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(whatsappConfig));

        ResponseEntity<String> response = controller.verifyWebhook("subscribe", "test-webhook-secret", "challenge-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("challenge-123");
    }

    @Test
    void verifyWebhookReturns403WhenTokenMismatch() {
        when(channelConfigRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(whatsappConfig));

        ResponseEntity<String> response = controller.verifyWebhook("subscribe", "wrong-token", "challenge-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void verifyWebhookReturns403WhenModeNotSubscribe() {
        when(channelConfigRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(whatsappConfig));

        ResponseEntity<?> response = controller.verifyWebhook("unsubscribe", "test-webhook-secret", "challenge-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleWhatsAppMessageReturns403WhenSignatureInvalid() throws Exception {
        when(channelConfigRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(whatsappConfig));

        String body = "{\"object\":\"whatsapp_business_account\",\"entry\":[{\"changes\":[{\"value\":{\"messaging_product\":\"whatsapp\",\"messages\":[{\"from\":\"201012345678\",\"text\":{\"body\":\"Hello\"},\"type\":\"text\"}],\"contacts\":[{\"wa_id\":\"201012345678\",\"profile\":{\"name\":\"Ahmed\"}}]}}]}]}";
        String invalidSignature = "sha256=" + computeHmac(body, "wrong-secret");

        ResponseEntity<?> response = controller.handleWhatsAppMessage(body, invalidSignature, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleWhatsAppMessageProcessesValidSignature() throws Exception {
        when(channelConfigRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(whatsappConfig));
        when(crmService.handleInboundWebhook(any())).thenReturn(mock(CrmApi.MessageResponse.class));

        String body = "{\"object\":\"whatsapp_business_account\",\"entry\":[{\"changes\":[{\"value\":{\"messaging_product\":\"whatsapp\",\"messages\":[{\"from\":\"201012345678\",\"text\":{\"body\":\"Hello\"},\"type\":\"text\"}],\"contacts\":[{\"wa_id\":\"201012345678\",\"profile\":{\"name\":\"Ahmed\"}}]}}]}]}";
        String validSignature = "sha256=" + computeHmac(body, "test-webhook-secret");

        ResponseEntity<?> response = controller.handleWhatsAppMessage(body, validSignature, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(crmService, times(1)).handleInboundWebhook(any());
    }

    @Test
    void handleWhatsAppMessageReturns503WhenNoChannelConfigured() {
        when(channelConfigRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        ResponseEntity<?> response = controller.handleWhatsAppMessage("{}", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private String computeHmac(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
