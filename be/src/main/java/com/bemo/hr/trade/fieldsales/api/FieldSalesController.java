package com.bemo.hr.trade.fieldsales.api;

import com.bemo.hr.trade.fieldsales.application.FieldSalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/trade/field-sales")
@RequiredArgsConstructor
public class FieldSalesController {

    private final FieldSalesService fieldSalesService;

    @GetMapping("/offline-bundle")
    @PreAuthorize("@auth.hasAnyPermission('fieldSales:read', 'fieldSales:operate', 'sales:read', 'sales:manage')")
    public FieldSalesApi.OfflineBundleResponse getOfflineBundle(
            @RequestParam(required = false) String salesRepUserId,
            Authentication authentication
    ) {
        String effectiveUserId = salesRepUserId != null && !salesRepUserId.isBlank()
                ? salesRepUserId
                : (authentication != null ? authentication.getName() : "anonymous");
        log.debug("GET /api/v1/trade/field-sales/offline-bundle for user {}", effectiveUserId);
        return fieldSalesService.getOfflineBundle(effectiveUserId);
    }

    @PostMapping("/sync")
    @PreAuthorize("@auth.hasAnyPermission('fieldSales:operate', 'fieldSales:manage', 'sales:manage')")
    public FieldSalesApi.SyncBatchResponse syncBatch(
            @Valid @RequestBody FieldSalesApi.SyncBatchRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "anonymous";
        log.info("POST /api/v1/trade/field-sales/sync received {} transactions from {}", request.transactions().size(), actor);
        return fieldSalesService.syncBatch(request, actor, actor);
    }

    @GetMapping("/history")
    @PreAuthorize("@auth.hasAnyPermission('fieldSales:read', 'fieldSales:operate', 'sales:read', 'sales:manage')")
    public List<FieldSalesApi.OfflineTransactionRecordResponse> getSyncHistory(
            @RequestParam(required = false) String salesRepUserId,
            Authentication authentication
    ) {
        String effectiveUserId = salesRepUserId != null && !salesRepUserId.isBlank()
                ? salesRepUserId
                : (authentication != null ? authentication.getName() : "anonymous");
        log.debug("GET /api/v1/trade/field-sales/history for user {}", effectiveUserId);
        return fieldSalesService.getSyncHistory(effectiveUserId);
    }
}
