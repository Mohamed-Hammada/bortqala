package com.bemo.hr.platform;

import com.bemo.hr.platform.api.PlatformApi;
import com.bemo.hr.platform.application.WebhookService;
import com.bemo.hr.platform.domain.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTests {

    @Mock WebhookEndpointRepository endpointRepo;
    @Mock WebhookDeliveryRepository deliveryRepo;
    WebhookService service;

    @BeforeEach
    void setUp() {
        service = new WebhookService(endpointRepo, deliveryRepo);
    }

    @Test
    void createEndpoint_persistsAndReturns() {
        when(endpointRepo.findByAppIdOrderByCreatedAtDesc("app-1")).thenReturn(List.of());
        when(endpointRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new PlatformApi.WebhookEndpointCreateRequest("https://example.com/hook", "invoice.paid");
        var result = service.createEndpoint("app-1", req);

        assertNotNull(result.id());
        assertEquals("https://example.com/hook", result.url());
        assertEquals("invoice.paid", result.events());
        assertTrue(result.active());
    }

    @Test
    void createEndpoint_limitReached_throws() {
        List<WebhookEndpoint> existing = java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> mock(WebhookEndpoint.class))
                .toList();
        when(endpointRepo.findByAppIdOrderByCreatedAtDesc("app-1")).thenReturn(existing);

        var req = new PlatformApi.WebhookEndpointCreateRequest("https://x.com", "");
        assertThrows(BusinessRuleException.class, () -> service.createEndpoint("app-1", req));
    }

    @Test
    void toggleEndpoint_notFound_throws() {
        when(endpointRepo.findById("bad")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.toggleEndpoint("app-1", "bad", false));
    }

    @Test
    void toggleEndpoint_wrongApp_throws() {
        WebhookEndpoint ep = mock(WebhookEndpoint.class);
        when(endpointRepo.findById("id-1")).thenReturn(Optional.of(ep));
        when(ep.getAppId()).thenReturn("other-app");
        assertThrows(NotFoundException.class, () -> service.toggleEndpoint("app-1", "id-1", false));
    }

    @Test
    void listDeliveries_notFound_throws() {
        when(endpointRepo.findById("bad")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.listDeliveries("app-1", "bad"));
    }

    @Test
    void redriveDelivery_pendingAgain() {
        WebhookEndpoint ep = mock(WebhookEndpoint.class);
        when(endpointRepo.findById("ep-1")).thenReturn(Optional.of(ep));
        when(ep.getAppId()).thenReturn("app-1");

        WebhookDelivery delivery = mock(WebhookDelivery.class);
        when(deliveryRepo.findById(1L)).thenReturn(Optional.of(delivery));
        when(delivery.getEndpointId()).thenReturn("ep-1");
        when(deliveryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.redriveDelivery("app-1", "ep-1", 1L);
        verify(delivery).setStatus("PENDING");
        verify(delivery).setAttempts(0);
        verify(delivery).setLastError(null);
    }

    @Test
    void redriveDelivery_endpointMismatch_throws() {
        WebhookEndpoint ep = mock(WebhookEndpoint.class);
        when(endpointRepo.findById("ep-1")).thenReturn(Optional.of(ep));
        when(ep.getAppId()).thenReturn("app-1");

        WebhookDelivery delivery = mock(WebhookDelivery.class);
        when(deliveryRepo.findById(1L)).thenReturn(Optional.of(delivery));
        when(delivery.getEndpointId()).thenReturn("other-ep");

        assertThrows(BusinessRuleException.class, () -> service.redriveDelivery("app-1", "ep-1", 1L));
    }
}
