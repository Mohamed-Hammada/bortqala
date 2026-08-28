package com.bemo.hr.finance.paylink.application;

import com.bemo.hr.finance.paylink.api.PaylinkApi;
import com.bemo.hr.finance.paylink.domain.*;
import com.bemo.hr.notification.BusinessNotification;
import com.bemo.hr.notification.BusinessNotificationRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
@RequiredArgsConstructor
public class PaymentLinkService {

    private final PaymentLinkRepository linkRepo;
    private final GatewayTransactionRepository txnRepo;
    private final PaymentGatewayClient gatewayClient;
    private final BusinessNotificationRepository notificationRepo;

    @Value("${hr.payments.gateway:NONE}")
    private String gatewayType;

    @Value("${hr.payments.link-ttl-hours:48}")
    private long linkTtlHours;

    @Value("${hr.payments.webhook-secret:}")
    private String webhookSecret;

    public boolean isGatewayEnabled() {
        return !"NONE".equalsIgnoreCase(gatewayType);
    }

    @Transactional
    public PaylinkApi.LinkResponse createLink(String appId, PaylinkApi.CreateLinkPayload payload, String companyName) {
        if (!isGatewayEnabled())
            throw new BusinessRuleException("Payment gateways are not configured.",
                    "PAYLINK_GATEWAY_OFF", HttpStatus.SERVICE_UNAVAILABLE);
        Instant expiresAt = payload.expiresAtEpochMs() != null
                ? Instant.ofEpochMilli(payload.expiresAtEpochMs())
                : Instant.now().plus(Duration.ofHours(linkTtlHours));
        PaymentLink link = new PaymentLink(appId, PaymentLink.Kind.valueOf(payload.kind().toUpperCase()),
                payload.refId(), payload.amount(), payload.description(), companyName, expiresAt);
        linkRepo.save(link);
        return toResponse(link);
    }

    @Transactional(readOnly = true)
    public List<PaylinkApi.LinkResponse> listLinks(String appId) {
        return linkRepo.findByAppIdOrderByCreatedAtDesc(appId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public void cancelLink(String appId, String linkId) {
        PaymentLink link = linkRepo.findById(linkId)
                .filter(l -> l.getAppId().equals(appId))
                .orElseThrow(() -> new BusinessRuleException("Payment link not found.",
                        "PAYLINK_NOT_FOUND", HttpStatus.NOT_FOUND));
        link.cancel();
        linkRepo.save(link);
    }

    @Transactional(readOnly = true)
    public PaylinkApi.PublicPagePayload getPublicPage(String token) {
        PaymentLink link = linkRepo.findByToken(token)
                .orElseThrow(() -> new BusinessRuleException("Payment link not found.",
                        "PAYLINK_NOT_FOUND", HttpStatus.NOT_FOUND));
        return new PaylinkApi.PublicPagePayload(link.getCompanyName(), link.getDescription(),
                link.getAmount(), link.getCurrency(), link.isExpired(), link.getStatus() == PaymentLink.Status.PAID);
    }

    @Transactional
    public void handleWebhook(String token, PaylinkApi.WebhookPayload payload) {
        PaymentLink link = linkRepo.findByToken(token)
                .orElseThrow(() -> new BusinessRuleException("Payment link not found.",
                        "PAYLINK_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (!link.canReceivePayment()) {
            if (link.isExpired())
                throw new BusinessRuleException("This payment link has expired.",
                        "PAYLINK_EXPIRED", HttpStatus.GONE);
            throw new BusinessRuleException("Link is not in PENDING status.",
                    "PAYLINK_INVALID_STATE", HttpStatus.CONFLICT);
        }
        verifySignature(payload);
        PaymentGatewayClient.WebhookResult result = gatewayClient.verifyWebhook(
                payload.providerTxnId(), payload.signature());
        // Idempotent: skip if this provider txn was already processed
        if (txnRepo.findByAppIdAndProviderTxnId(link.getAppId(), result.providerTxnId()).isPresent()) return;
        GatewayTransaction txn = new GatewayTransaction(link.getAppId(), link.getId(),
                result.providerTxnId(), result.rawPayload(), result.amount(), Instant.now());
        txnRepo.save(txn);
        link.confirm(result.providerTxnId(), Instant.now());
        linkRepo.save(link);
        BusinessNotification notification = new BusinessNotification(
                "admin", "تم استلام الدفع", "Payment Received",
                "تم استلام دفعة بمبلغ " + link.getAmount() + " ج.م",
                "Payment of " + link.getAmount() + " EGP received",
                "PAYMENT_RECEIVED", "INFO", null);
        notificationRepo.save(notification);
    }

    @Transactional
    public void runExpirationScan() {
        List<PaymentLink> expired = linkRepo.findExpiredPending(
                linkRepo.findAll().stream().findFirst().map(PaymentLink::getAppId).orElse(""));
        for (PaymentLink link : expired) {
            link.expire();
            linkRepo.save(link);
        }
    }

    /**
     * HMAC-SHA256 signature verification over the canonical webhook payload (the provider
     * transaction id). Only enforced when {@code hr.payments.webhook-secret} is configured;
     * a tampered/missing signature is rejected with 401 {@code WEBHOOK_SIGNATURE_INVALID}
     * before any state change (WP-29 AC-2).
     */
    private void verifySignature(PaylinkApi.WebhookPayload payload) {
        if (webhookSecret == null || webhookSecret.isBlank()) return;
        if (payload.signature() == null || payload.signature().isBlank()) {
            throw new BusinessRuleException("Webhook signature is missing.",
                    "WEBHOOK_SIGNATURE_INVALID", HttpStatus.UNAUTHORIZED);
        }
        String expected = hmacSha256(webhookSecret, payload.providerTxnId());
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                payload.signature().getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessRuleException("Webhook signature is invalid.",
                    "WEBHOOK_SIGNATURE_INVALID", HttpStatus.UNAUTHORIZED);
        }
    }

    private String hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable.", e);
        }
    }

    private PaylinkApi.LinkResponse toResponse(PaymentLink link) {
        return new PaylinkApi.LinkResponse(
                link.getId(), link.getKind().name(), link.getRefId(), link.getAmount(), link.getCurrency(),
                link.getToken(), link.getStatus().name(), link.getGatewayRef(), link.getDescription(),
                link.getCompanyName(),
                link.getExpiresAt() != null ? link.getExpiresAt().toEpochMilli() : null,
                link.getPaidAt() != null ? link.getPaidAt().toEpochMilli() : null,
                null, link.getVersion());
    }
}
