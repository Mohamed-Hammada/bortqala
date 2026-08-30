package com.bemo.hr.notification.push;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushService {
    private final WebPushProperties properties;
    private final WebPushSubscriptionRepository subscriptionRepository;
    private final JdbcTemplate jdbcTemplate;
    private volatile PushService cachedPushService;

    private static String endpointHash(String endpoint) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(endpoint.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception error) {
            throw new IllegalStateException("Unable to hash Web Push endpoint", error);
        }
    }

    private static String json(String value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder(value.length() + 16).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    public WebPushApi.ConfigResponse config() {
        boolean enabled = properties.configured();
        return new WebPushApi.ConfigResponse(enabled, enabled ? properties.getPublicKey().trim() : "");
    }

    @Transactional
    public WebPushApi.SubscriptionStatus register(String username, WebPushApi.SubscriptionPayload payload) {
        boolean android = payload.isAndroid();
        if (!android) requireConfigured();
        String endpoint = resolveEndpoint(payload);
        String hash = endpointHash(endpoint);
        String p256dh = payload.keys() == null ? "fcm:p256dh" : orBlankMarker(payload.keys().p256dh());
        String auth = payload.keys() == null ? "fcm:auth" : orBlankMarker(payload.keys().auth());
        WebPushSubscription subscription = subscriptionRepository.findByEndpointHash(hash)
                .orElseGet(() -> new WebPushSubscription(
                        username, endpoint, hash, p256dh, auth,
                        payload.locale(), payload.pushApprovals(), payload.pushPayroll(), payload.platform(), payload.fcmToken()));
        subscription.update(username, endpoint, hash, p256dh, auth,
                payload.locale(), payload.pushApprovals(), payload.pushPayroll());
        if (android) subscription.registerFcmToken(payload.fcmToken());
        subscriptionRepository.save(subscription);
        return new WebPushApi.SubscriptionStatus(true);
    }

    private String orBlankMarker(String value) {
        return value == null || value.isBlank() ? "fcm:unused" : value.strip();
    }

    private String resolveEndpoint(WebPushApi.SubscriptionPayload payload) {
        if (!payload.isAndroid()) return payload.endpoint();
        if (payload.endpoint() != null && !payload.endpoint().isBlank()) return payload.endpoint();
        return "android://fcm/" + endpointHash(payload.fcmToken()).substring(0, 32);
    }

    @Transactional
    public void unsubscribe(String username, WebPushApi.UnsubscribePayload payload) {
        subscriptionRepository.findByEndpointHash(endpointHash(payload.endpoint()))
                .filter(subscription -> subscription.belongsTo(username))
                .ifPresent(subscription -> {
                    subscription.disable();
                    subscriptionRepository.save(subscription);
                });
    }

    @Transactional
    public void unsubscribeAll(String username) {
        subscriptionRepository.findByUsernameIgnoreCaseAndEnabledTrue(username).forEach(subscription -> {
            subscription.disable();
            subscriptionRepository.save(subscription);
        });
    }

    @Transactional(readOnly = true)
    public WebPushApi.SubscriptionStatus status(String username) {
        return new WebPushApi.SubscriptionStatus(
                !subscriptionRepository.findByUsernameIgnoreCaseAndEnabledTrue(username).isEmpty());
    }

    @Transactional
    public void deliver(NotificationCreatedEvent event) {
        if (!properties.configured() || event.appId() == null || event.appId().isBlank()) return;
        List<DeliveryTarget> targets = jdbcTemplate.query("""
                select id, endpoint, p256dh_key, auth_key, locale, push_approvals, push_payroll
                  from web_push_subscriptions
                 where app_id = ? and lower(username) = lower(?) and enabled = true and platform = 'WEB'
                """, (ResultSet rs, int rowNum) -> new DeliveryTarget(
                rs.getString("id"), rs.getString("endpoint"), rs.getString("p256dh_key"),
                rs.getString("auth_key"), rs.getString("locale"), rs.getBoolean("push_approvals"),
                rs.getBoolean("push_payroll")), event.appId(), event.recipientUsername());

        for (DeliveryTarget target : targets) {
            if (!allows(target, event.notificationType())) continue;
            try {
                HttpResponse response = pushService().send(new Notification(
                        target.endpoint(), target.p256dhKey(), target.authKey(),
                        payloadFor(event, target.locale()).getBytes(StandardCharsets.UTF_8), properties.safeTtlSeconds()));
                int status = response.getStatusLine().getStatusCode();
                EntityUtils.consumeQuietly(response.getEntity());
                if (status >= 200 && status < 300) {
                    jdbcTemplate.update("update web_push_subscriptions set failure_count=0,last_success_at=?,updated_at=? where id=? and app_id=?",
                            Instant.now(), Instant.now(), target.id(), event.appId());
                } else if (status == 404 || status == 410) {
                    jdbcTemplate.update("update web_push_subscriptions set enabled=false,updated_at=? where id=? and app_id=?",
                            Instant.now(), target.id(), event.appId());
                } else {
                    markFailure(target.id(), event.appId());
                    log.warn("Web Push delivery HTTP {} app={} user={}", status, event.appId(), event.recipientUsername());
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                markFailure(target.id(), event.appId());
            } catch (Exception error) {
                markFailure(target.id(), event.appId());
                log.warn("Web Push delivery failed app={} user={}: {}", event.appId(), event.recipientUsername(), error.getMessage());
            }
        }
    }

    private void markFailure(String id, String appId) {
        jdbcTemplate.update("update web_push_subscriptions set failure_count=failure_count+1,last_failure_at=?,updated_at=? where id=? and app_id=?",
                Instant.now(), Instant.now(), id, appId);
    }

    private boolean allows(DeliveryTarget target, String notificationType) {
        String type = notificationType == null ? "" : notificationType.toUpperCase(Locale.ROOT);
        if (type.contains("APPROVAL")) return target.pushApprovals();
        if (type.contains("PAYROLL")) return target.pushPayroll();
        return true;
    }

    private PushService pushService() throws Exception {
        PushService local = cachedPushService;
        if (local != null) return local;
        synchronized (this) {
            if (cachedPushService == null) {
                if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                }
                cachedPushService = new PushService(properties.getPublicKey().trim(), properties.getPrivateKey().trim(), properties.getSubject().trim());
            }
            return cachedPushService;
        }
    }

    private String payloadFor(NotificationCreatedEvent event, String locale) {
        boolean english = locale != null && locale.toLowerCase(Locale.ROOT).startsWith("en");
        String title = english ? event.titleEn() : event.titleAr();
        String body = english ? event.messageEn() : event.messageAr();
        String url = event.actionLink() != null && event.actionLink().startsWith("/") ? event.actionLink() : "/";
        return """
                {"notification":{"title":%s,"body":%s,"lang":%s,"dir":%s,"tag":%s,"data":{"notificationId":%s,"type":%s,"url":%s,"onActionClick":{"default":{"operation":"openWindow","url":%s}}}}}
                """.formatted(json(title), json(body), json(english ? "en" : "ar"), json(english ? "ltr" : "rtl"),
                json("bemo-" + event.id()), json(event.id()), json(event.notificationType()), json(url), json(url)).trim();
    }

    private void requireConfigured() {
        if (!properties.configured())
            throw new IllegalStateException("Web Push is not configured. Set HR_WEB_PUSH_ENABLED and VAPID keys.");
    }

    private record DeliveryTarget(String id, String endpoint, String p256dhKey, String authKey,
                                  String locale, boolean pushApprovals, boolean pushPayroll) {
    }
}
