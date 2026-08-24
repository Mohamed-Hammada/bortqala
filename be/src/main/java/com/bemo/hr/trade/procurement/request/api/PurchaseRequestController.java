package com.bemo.hr.trade.procurement.request.api;

import com.bemo.hr.trade.procurement.request.application.PurchaseRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase-requests")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    public PurchaseRequestController(PurchaseRequestService purchaseRequestService) {
        this.purchaseRequestService = purchaseRequestService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<PurchaseRequestApi.PurchaseRequestResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String departmentId) {
        return purchaseRequestService.list(status, departmentId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PurchaseRequestApi.PurchaseRequestResponse get(@PathVariable String id) {
        return purchaseRequestService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('procurement.manage') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public PurchaseRequestApi.PurchaseRequestResponse create(
            @Valid @RequestBody PurchaseRequestApi.PurchaseRequestPayload payload) {
        return purchaseRequestService.create(payload);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@auth.hasPermission('procurement.manage') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public PurchaseRequestApi.PurchaseRequestResponse update(@PathVariable String id,
                                                             @Valid @RequestBody PurchaseRequestApi.PurchaseRequestPayload payload) {
        return purchaseRequestService.update(id, payload);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@auth.hasPermission('procurement.manage') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public PurchaseRequestApi.PurchaseRequestResponse submit(@PathVariable String id) {
        return purchaseRequestService.submit(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER','ADMIN','SUPER_ADMIN')")
    public PurchaseRequestApi.PurchaseRequestResponse approve(@PathVariable String id,
                                                              @RequestBody(required = false) ApprovalNote note) {
        return purchaseRequestService.approve(id, note == null ? null : note.note());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER','ADMIN','SUPER_ADMIN')")
    public PurchaseRequestApi.PurchaseRequestResponse reject(@PathVariable String id,
                                                             @RequestBody(required = false) ApprovalNote note) {
        return purchaseRequestService.reject(id, note == null ? null : note.note());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@auth.hasPermission('procurement.manage') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public PurchaseRequestApi.PurchaseRequestResponse cancel(@PathVariable String id) {
        return purchaseRequestService.cancel(id);
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAnyRole('PROCUREMENT_MANAGER','ADMIN','SUPER_ADMIN')")
    public PurchaseRequestApi.PurchaseRequestResponse convert(@PathVariable String id,
                                                              @Valid @RequestBody PurchaseRequestApi.ConvertRequest request) {
        return purchaseRequestService.convert(id, request.supplierId());
    }

    public record ApprovalNote(String note) {
    }
}
