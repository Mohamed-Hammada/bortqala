package com.bemo.hr.platform.api;

import com.bemo.hr.platform.application.ApiKeyService;
import com.bemo.hr.platform.application.GridViewService;
import com.bemo.hr.platform.application.SearchService;
import com.bemo.hr.platform.application.WebhookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/platform")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class PlatformController {

    private final ApiKeyService apiKeyService;
    private final WebhookService webhookService;
    private final SearchService searchService;
    private final GridViewService gridViewService;

    public PlatformController(ApiKeyService apiKeyService, WebhookService webhookService,
                              SearchService searchService, GridViewService gridViewService) {
        this.apiKeyService = apiKeyService;
        this.webhookService = webhookService;
        this.searchService = searchService;
        this.gridViewService = gridViewService;
    }

    @GetMapping("/search")
    public PlatformApi.SearchResponse search(@RequestParam String q, Authentication auth) {
        return searchService.search(q, resolveAppId(auth));
    }

    @PostMapping("/grid-views")
    @ResponseStatus(HttpStatus.CREATED)
    public GridViewApi.GridViewResponse saveGridView(
            @Valid @RequestBody GridViewApi.GridViewSaveRequest request,
            Authentication auth) {
        return gridViewService.saveView(resolveAppId(auth), auth.getName(), request);
    }

    @GetMapping("/grid-views")
    public GridViewApi.GridViewListResponse listGridViews(
            @RequestParam String pageKey, Authentication auth) {
        return gridViewService.listViews(resolveAppId(auth), auth.getName(), pageKey);
    }

    @DeleteMapping("/grid-views/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGridView(@PathVariable String id, Authentication auth) {
        gridViewService.deleteView(resolveAppId(auth), auth.getName(), id);
    }

    @PostMapping("/api-keys")
    @ResponseStatus(HttpStatus.CREATED)
    public PlatformApi.ApiKeyCreateResponse createApiKey(
            @Valid @RequestBody PlatformApi.ApiKeyCreateRequest request,
            Authentication auth) {
        return apiKeyService.createKey(resolveAppId(auth), request, auth.getName());
    }

    @GetMapping("/api-keys")
    public PlatformApi.ApiKeyListResponse listApiKeys(Authentication auth) {
        return apiKeyService.listKeys(resolveAppId(auth));
    }

    @PostMapping("/api-keys/{id}/toggle")
    public void toggleApiKey(@PathVariable String id, @RequestParam boolean active, Authentication auth) {
        apiKeyService.toggleKey(resolveAppId(auth), id, active);
    }

    @PostMapping("/api-keys/{id}/revoke")
    public void revokeApiKey(@PathVariable String id, Authentication auth) {
        apiKeyService.revokeKey(resolveAppId(auth), id);
    }

    @DeleteMapping("/api-keys/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApiKey(@PathVariable String id, Authentication auth) {
        apiKeyService.deleteKey(resolveAppId(auth), id);
    }

    @PostMapping("/webhooks")
    @ResponseStatus(HttpStatus.CREATED)
    public PlatformApi.WebhookEndpointResponse createWebhook(
            @Valid @RequestBody PlatformApi.WebhookEndpointCreateRequest request,
            Authentication auth) {
        return webhookService.createEndpoint(resolveAppId(auth), request);
    }

    @GetMapping("/webhooks")
    public PlatformApi.WebhookEndpointListResponse listWebhooks(Authentication auth) {
        return webhookService.listEndpoints(resolveAppId(auth));
    }

    @PostMapping("/webhooks/{id}/toggle")
    public void toggleWebhook(@PathVariable String id, @RequestParam boolean active, Authentication auth) {
        webhookService.toggleEndpoint(resolveAppId(auth), id, active);
    }

    @DeleteMapping("/webhooks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWebhook(@PathVariable String id, Authentication auth) {
        webhookService.deleteEndpoint(resolveAppId(auth), id);
    }

    @GetMapping("/webhooks/{endpointId}/deliveries")
    public PlatformApi.WebhookDeliveryListResponse listDeliveries(
            @PathVariable String endpointId, Authentication auth) {
        return webhookService.listDeliveries(resolveAppId(auth), endpointId);
    }

    @PostMapping("/webhooks/{endpointId}/deliveries/{deliveryId}/redrive")
    public void redriveDelivery(@PathVariable String endpointId, @PathVariable Long deliveryId, Authentication auth) {
        webhookService.redriveDelivery(resolveAppId(auth), endpointId, deliveryId);
    }

    private String resolveAppId(Authentication auth) {
        var details = auth.getDetails();
        if (details instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            return jwt.getClaimAsString("appId");
        }
        return "";
    }
}
