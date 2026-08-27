package com.bemo.hr.platform.application;

import com.bemo.hr.platform.api.PlatformApi;
import com.bemo.hr.platform.domain.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@Transactional
public class WebhookService {

    private static final int MAX_ENDPOINTS = 20;
    private static final int MAX_DELIVERIES_PER_ENDPOINT = 100;

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;

    public WebhookService(WebhookEndpointRepository endpointRepository,
                          WebhookDeliveryRepository deliveryRepository) {
        this.endpointRepository = endpointRepository;
        this.deliveryRepository = deliveryRepository;
    }

    public PlatformApi.WebhookEndpointResponse createEndpoint(String appId, PlatformApi.WebhookEndpointCreateRequest request) {
        long count = endpointRepository.findByAppIdOrderByCreatedAtDesc(appId).size();
        if (count >= MAX_ENDPOINTS) {
            throw new BusinessRuleException("Maximum webhook endpoints limit reached", "WEBHOOK_LIMIT_REACHED", HttpStatus.CONFLICT);
        }
        String secret = generateSecret();
        String events = request.events() != null ? request.events() : "";
        WebhookEndpoint endpoint = new WebhookEndpoint(appId, request.url(), secret, events);
        endpointRepository.save(endpoint);
        return toEndpointResponse(endpoint);
    }

    public PlatformApi.WebhookEndpointListResponse listEndpoints(String appId) {
        List<PlatformApi.WebhookEndpointResponse> endpoints = endpointRepository.findByAppIdOrderByCreatedAtDesc(appId)
                .stream().map(this::toEndpointResponse).toList();
        return new PlatformApi.WebhookEndpointListResponse(endpoints);
    }

    public void toggleEndpoint(String appId, String endpointId, boolean active) {
        WebhookEndpoint ep = endpointRepository.findById(endpointId)
                .filter(e -> e.getAppId().equals(appId))
                .orElseThrow(() -> new NotFoundException("Webhook endpoint not found", "WEBHOOK_NOT_FOUND"));
        ep.setActive(active);
        endpointRepository.save(ep);
    }

    public void deleteEndpoint(String appId, String endpointId) {
        WebhookEndpoint ep = endpointRepository.findById(endpointId)
                .filter(e -> e.getAppId().equals(appId))
                .orElseThrow(() -> new NotFoundException("Webhook endpoint not found", "WEBHOOK_NOT_FOUND"));
        endpointRepository.delete(ep);
    }

    public PlatformApi.WebhookDeliveryListResponse listDeliveries(String appId, String endpointId) {
        WebhookEndpoint ep = endpointRepository.findById(endpointId)
                .filter(e -> e.getAppId().equals(appId))
                .orElseThrow(() -> new NotFoundException("Webhook endpoint not found", "WEBHOOK_NOT_FOUND"));
        List<PlatformApi.WebhookDeliveryResponse> deliveries = deliveryRepository.findByEndpointIdOrderByCreatedAtDesc(ep.getId())
                .stream().limit(MAX_DELIVERIES_PER_ENDPOINT).map(this::toDeliveryResponse).toList();
        return new PlatformApi.WebhookDeliveryListResponse(deliveries);
    }

    public void redriveDelivery(String appId, String endpointId, Long deliveryId) {
        endpointRepository.findById(endpointId)
                .filter(e -> e.getAppId().equals(appId))
                .orElseThrow(() -> new NotFoundException("Webhook endpoint not found", "WEBHOOK_NOT_FOUND"));
        WebhookDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Webhook delivery not found", "WEBHOOK_DELIVERY_NOT_FOUND"));
        if (!delivery.getEndpointId().equals(endpointId)) {
            throw new BusinessRuleException("Delivery does not belong to this endpoint", "WEBHOOK_DELIVERY_ENDPOINT_MISMATCH", HttpStatus.CONFLICT);
        }
        delivery.setStatus("PENDING");
        delivery.setAttempts(0);
        delivery.setLastError(null);
        deliveryRepository.save(delivery);
    }

    private PlatformApi.WebhookEndpointResponse toEndpointResponse(WebhookEndpoint ep) {
        return new PlatformApi.WebhookEndpointResponse(
                ep.getId(), ep.getUrl(), ep.getEvents(),
                ep.isActive(), ep.getCreatedAt().toEpochMilli(),
                ep.getUpdatedAt().toEpochMilli(), ep.getVersion()
        );
    }

    private PlatformApi.WebhookDeliveryResponse toDeliveryResponse(WebhookDelivery d) {
        return new PlatformApi.WebhookDeliveryResponse(
                d.getId(), d.getEndpointId(), d.getEvent(), d.getPayload(),
                d.getStatus(), d.getAttempts(), d.getLastError(),
                d.getResponseStatus(), d.getCreatedAt()
        );
    }

    private static String generateSecret() {
        byte[] random = new byte[32];
        new java.security.SecureRandom().nextBytes(random);
        return "whsec_" + HexFormat.of().formatHex(random);
    }
}
