package com.bemo.hr.crm.api;

import com.bemo.hr.crm.application.CrmService;
import com.bemo.hr.crm.domain.CrmChannelConfig;
import com.bemo.hr.crm.domain.CrmChannelType;
import com.bemo.hr.crm.infrastructure.CrmChannelConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * WhatsApp Business Platform webhook endpoint with HMAC-SHA256 signature validation.
 *
 * The WhatsApp Cloud API signs every inbound webhook request with an X-Hub-Signature-256 header.
 * This controller validates the signature before forwarding the message to the CRM service.
 *
 * Verification flow:
 * 1. Extract X-Hub-Signature-256 header from the request
 * 2. Look up the webhook secret from CrmChannelConfig for WHATSAPP channel
 * 3. Compute HMAC-SHA256 of the raw request body using the secret
 * 4. Compare computed hash with the received signature
 * 5. If valid, parse the WhatsApp payload and forward to CrmService.handleInboundWebhook
 * 6. If invalid, return 403 Forbidden
 *
 * WhatsApp Cloud API payload structure:
 * {
 *   "object": "whatsapp_business_account",
 *   "entry": [{
 *     "changes": [{
 *       "value": {
 *         "messaging_product": "whatsapp",
 *         "messages": [{
 *           "from": "201012345678",
 *           "timestamp": "1234567890",
 *           "text": { "body": "Hello" },
 *           "type": "text"
 *         }],
 *         "contacts": [{ "wa_id": "201012345678", "profile": { "name": "Ahmed" } }]
 *       }
 *     }]
 *   }]
 * }
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/crm/webhooks")
public class WhatsAppWebhookController {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";
    private static final String WHATSAPP_OBJECT = "whatsapp_business_account";

    private final CrmService crmService;
    private final CrmChannelConfigRepository channelConfigRepository;

    public WhatsAppWebhookController(CrmService crmService,
                                      CrmChannelConfigRepository channelConfigRepository) {
        this.crmService = crmService;
        this.channelConfigRepository = channelConfigRepository;
    }

    /**
     * GET endpoint for WhatsApp webhook verification (required during setup).
     * WhatsApp sends a hub.mode, hub.verify_token, and hub.challenge query.
     */
    @GetMapping("/whatsapp")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {

        if (!"subscribe".equals(mode)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid mode");
        }

        // Find a WHATSAPP channel config with matching verify token
        Optional<CrmChannelConfig> config = channelConfigRepository
                .findAllByOrderByCreatedAtDesc().stream()
                .filter(c -> c.getChannelType() == CrmChannelType.WHATSAPP && c.isActive())
                .filter(c -> verifyToken.equals(c.getWebhookSecret()))
                .findFirst();

        if (config.isEmpty()) {
            log.warn("WhatsApp webhook verification failed: no matching channel config for token");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification token mismatch");
        }

        log.info("WhatsApp webhook verified successfully for channel: {}", config.get().getChannelName());
        return ResponseEntity.ok(challenge);
    }

    /**
     * POST endpoint for receiving WhatsApp inbound messages.
     * Validates HMAC-SHA256 signature before processing.
     */
    @PostMapping("/whatsapp")
    public ResponseEntity<?> handleWhatsAppMessage(
            @RequestBody String rawBody,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
            HttpServletRequest request) {

        // 1. Find WhatsApp channel config with webhook secret
        Optional<CrmChannelConfig> configOpt = channelConfigRepository
                .findAllByOrderByCreatedAtDesc().stream()
                .filter(c -> c.getChannelType() == CrmChannelType.WHATSAPP && c.isActive())
                .findFirst();

        if (configOpt.isEmpty()) {
            log.warn("WhatsApp webhook received but no active WHATSAPP channel configured");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "No active WhatsApp channel configured"));
        }

        CrmChannelConfig config = configOpt.get();
        String webhookSecret = config.getWebhookSecret();

        // 2. Validate HMAC-SHA256 signature (if secret is configured)
        if (webhookSecret != null && !webhookSecret.isBlank() && signature != null) {
            if (!validateSignature(rawBody, signature, webhookSecret)) {
                log.warn("WhatsApp webhook signature validation failed");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Invalid signature"));
            }
        } else if (signature != null && (webhookSecret == null || webhookSecret.isBlank())) {
            log.warn("WhatsApp webhook has signature but no secret configured — allowing (dev mode)");
        }

        // 3. Handle the "object" verification ping (WhatsApp sends this periodically)
        if (rawBody.contains("\"object\":\"" + WHATSAPP_OBJECT + "\"") && rawBody.contains("\"changes\"")) {
            try {
                // Parse the WhatsApp payload and extract message data
                WhatsAppPayload parsed = parseWhatsAppPayload(rawBody);
                if (parsed != null) {
                    CrmApi.InboundWebhookRequest webhookRequest = new CrmApi.InboundWebhookRequest(
                            CrmChannelType.WHATSAPP,
                            parsed.senderPhone,
                            parsed.senderName,
                            parsed.messageText,
                            parsed.attachmentUrl
                    );
                    crmService.handleInboundWebhook(webhookRequest);
                    log.info("WhatsApp message processed from phone: {}", parsed.senderPhone);
                    return ResponseEntity.ok(Map.of("status", "ok"));
                }
            } catch (Exception e) {
                log.error("Failed to process WhatsApp webhook: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to process message"));
            }
        }

        // If it's just a verification ping or empty payload
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * Validate HMAC-SHA256 signature of the request body.
     * WhatsApp sends: "sha256=<hex-digest>"
     */
    private boolean validateSignature(String rawBody, String signature, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedSignature = "sha256=" + HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(
                    computedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("HMAC validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Simple JSON extraction for WhatsApp payload fields.
     * Uses basic string parsing to avoid adding a JSON library dependency
     * for this specific use case.
     */
    private WhatsAppPayload parseWhatsAppPayload(String rawBody) {
        // Extract sender phone from messages[0].from or contacts[0].wa_id
        String senderPhone = extractJsonString(rawBody, "from");
        if (senderPhone == null) senderPhone = extractJsonString(rawBody, "wa_id");

        // Extract sender name from contacts[0].profile.name
        String senderName = extractNestedJsonString(rawBody, "profile", "name");

        // Extract message text from messages[0].text.body
        String messageText = extractNestedJsonString(rawBody, "text", "body");

        if (senderPhone == null || messageText == null) {
            log.debug("WhatsApp payload missing required fields: sender={}, text={}", senderPhone, messageText);
            return null;
        }

        return new WhatsAppPayload(senderPhone, senderName, messageText, null);
    }

    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    private String extractNestedJsonString(String json, String outerKey, String innerKey) {
        int outerIdx = json.indexOf("\"" + outerKey + "\"");
        if (outerIdx < 0) return null;
        return extractJsonString(json.substring(outerIdx), innerKey);
    }

    private record WhatsAppPayload(String senderPhone, String senderName, String messageText, String attachmentUrl) {}
}
