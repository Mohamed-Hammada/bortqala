package com.bemo.hr.workforce;

import com.bemo.hr.shared.security.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/client-billing")
public class ClientBillingController {

    private final ClientBillingService clientBillingService;
    private final AuthService authService;

    public ClientBillingController(ClientBillingService clientBillingService, AuthService authService) {
        this.clientBillingService = clientBillingService;
        this.authService = authService;
    }

    // ------------------------------------------------------------------
    // Client rates
    // ------------------------------------------------------------------

    @GetMapping("/rates")
    @PreAuthorize("@auth.hasPermission('settlements.read')")
    public List<ClientBillingApi.RateResponse> listRates(
            @RequestParam(required = false) String clientPartyId) {
        return clientBillingService.listRates(clientPartyId);
    }

    @PostMapping("/rates")
    @PreAuthorize("@auth.hasPermission('settlements.prepare')")
    public ClientBillingApi.RateResponse addRate(@Valid @RequestBody ClientBillingApi.CreateRateRequest request) {
        return clientBillingService.addRate(request);
    }

    @DeleteMapping("/rates/{id}")
    @PreAuthorize("@auth.hasPermission('settlements.prepare')")
    public void deleteRate(@PathVariable String id) {
        clientBillingService.deleteRate(id);
    }

    // ------------------------------------------------------------------
    // Draft generation, review, confirmation
    // ------------------------------------------------------------------

    @PostMapping("/generate")
    @PreAuthorize("@auth.hasPermission('settlements.prepare')")
    public ClientBillingApi.BillingReviewResponse generate(@Valid @RequestBody ClientBillingApi.GenerateBillingRequest request) {
        return clientBillingService.generate(request);
    }

    @GetMapping("/{clientPartyId}/{period}")
    @PreAuthorize("@auth.hasPermission('settlements.read')")
    public ClientBillingApi.BillingReviewResponse review(@PathVariable String clientPartyId,
                                                         @PathVariable String period) {
        return clientBillingService.review(clientPartyId, period);
    }

    @PostMapping("/{clientPartyId}/{period}/confirm")
    @PreAuthorize("@auth.hasPermission('settlements.finalize')")
    public ClientBillingApi.ConfirmResponse confirm(@PathVariable String clientPartyId,
                                                    @PathVariable String period) {
        return clientBillingService.confirm(clientPartyId, period);
    }

    // ------------------------------------------------------------------
    // Margin reporting
    // ------------------------------------------------------------------

    @GetMapping("/{clientPartyId}/{period}/margin")
    @PreAuthorize("@auth.hasPermission('settlements.read')")
    public ClientBillingApi.MarginReportResponse margin(@PathVariable String clientPartyId,
                                                        @PathVariable String period) {
        return clientBillingService.marginReport(clientPartyId, period);
    }

    @GetMapping(value = "/{clientPartyId}/{period}/margin/export.xlsx", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("@auth.hasPermission('settlements.read')")
    public ResponseEntity<byte[]> exportMargin(@PathVariable String clientPartyId,
                                               @PathVariable String period,
                                               Authentication auth) {
        String locale = authService.currentPreferences(auth.getName()).locale();
        byte[] payload = clientBillingService.marginExport(clientPartyId, period, locale);
        String filename = "client-billing-margin-" + period + ".xlsx";
        String encoded = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded)
                .body(payload);
    }
}