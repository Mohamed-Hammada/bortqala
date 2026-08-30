package com.bemo.hr.notification.push;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebPushServiceRegistrationTests {

    private WebPushProperties properties;
    private WebPushSubscriptionRepository repository;
    private WebPushService service;

    @BeforeEach
    void setUp() {
        properties = new WebPushProperties();
        repository = mock(WebPushSubscriptionRepository.class);
        service = new WebPushService(properties, repository, null);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubNoExistingSubscription() {
        when(repository.findByEndpointHash(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(java.util.Optional.empty());
    }

    @Test
    void androidSubscriptionStoresFcmTokenWithoutVapidConfiguration() {
        stubNoExistingSubscription();
        properties.setEnabled(false);

        WebPushApi.SubscriptionPayload payload = new WebPushApi.SubscriptionPayload(
                "ANDROID", null, null, null,
                "fcm-token-123", "ar", true, false);

        WebPushApi.SubscriptionStatus status = service.register("merl", payload);

        assertThat(status.subscribed()).isTrue();
        org.mockito.ArgumentCaptor<WebPushSubscription> captor =
                org.mockito.ArgumentCaptor.forClass(WebPushSubscription.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPlatform()).isEqualTo("ANDROID");
        assertThat(captor.getValue().getFcmToken()).isEqualTo("fcm-token-123");
        assertThat(captor.getValue().getEndpoint()).startsWith("android://fcm/");
    }

    @Test
    void webSubscriptionStillRequiresVapidConfiguration() {
        stubNoExistingSubscription();
        properties.setEnabled(false);
        WebPushApi.SubscriptionPayload payload = new WebPushApi.SubscriptionPayload(
                "WEB", "https://push.example.com/abc", null,
                new WebPushApi.SubscriptionKeys("p", "a"), null, "en", false, false);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.register("merl", payload))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Web Push is not configured");
    }

    @Test
    void platformDefaultsToWebWhenBlank() {
        stubNoExistingSubscription();
        properties.setEnabled(true);
        properties.setPublicKey("k");
        properties.setPrivateKey("s");
        properties.setSubject("mailto:ops@bemo.erp");

        WebPushApi.SubscriptionPayload payload = new WebPushApi.SubscriptionPayload(
                null, "https://push.example.com/xyz", null,
                new WebPushApi.SubscriptionKeys("p", "a"), null, "en", false, false);

        service.register("merl", payload);
        org.mockito.ArgumentCaptor<WebPushSubscription> captor =
                org.mockito.ArgumentCaptor.forClass(WebPushSubscription.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPlatform()).isEqualTo("WEB");
    }
}
