package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.trade.procurement.application.ProcurementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.bemo.hr.shared.security.AuthService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/trade/procurement")
public class ProcurementController {

    private final ProcurementService procurementService;
    private final AuthService authService;

    public ProcurementController(ProcurementService procurementService, AuthService authService) {
        this.procurementService = procurementService;
        this.authService = authService;
    }

    @GetMapping(value = "/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> export(Authentication authentication) {
        var preference = authService.currentPreferences(authentication.getName());
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("procurement.xlsx", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(procurementService.export(preference.locale(), authentication.getName()));
    }

    // ─── Purchase Orders ────────────────────────────────────────────

    @GetMapping("/orders")
    public List<ProcurementApi.PurchaseOrderResponse> listPurchaseOrders() {
        return procurementService.list();
    }

    @GetMapping("/numbering-settings")
    public ProcurementApi.NumberingSettings numberingSettings() {
        return procurementService.numberingSettings();
    }

    @PostMapping("/orders")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public ProcurementApi.PurchaseOrderResponse createPurchaseOrder(
            @Valid @RequestBody ProcurementApi.PurchaseOrderPayload payload) {
        return procurementService.create(payload);
    }

    @PutMapping("/orders/{id}")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public ProcurementApi.PurchaseOrderResponse updatePurchaseOrder(
            @PathVariable String id, @Valid @RequestBody ProcurementApi.PurchaseOrderPayload payload) {
        return procurementService.update(id, payload);
    }

    @PostMapping("/orders/{id}/issue")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public ProcurementApi.PurchaseOrderResponse issuePurchaseOrder(@PathVariable String id) {
        return procurementService.issue(id);
    }

    @PostMapping("/orders/{id}/receive")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public ProcurementApi.PurchaseOrderResponse receivePurchaseOrder(@PathVariable String id) {
        return procurementService.receive(id);
    }

    @PostMapping("/orders/{id}/cancel")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public ProcurementApi.PurchaseOrderResponse cancelPurchaseOrder(@PathVariable String id) {
        return procurementService.cancel(id);
    }

    // ─── Goods Receipts ─────────────────────────────────────────────

    @GetMapping("/goods-receipts")
    public List<ProcurementApi.GoodsReceiptResponse> listGoodsReceipts() {
        return procurementService.listGoodsReceipts();
    }

    @PostMapping("/goods-receipts")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcurementApi.GoodsReceiptResponse createGoodsReceipt(
            @Valid @RequestBody ProcurementApi.GoodsReceiptPayload payload) {
        return procurementService.createGoodsReceipt(payload);
    }

    // ─── Supplier Invoices ──────────────────────────────────────────

    @GetMapping("/invoices")
    public List<ProcurementApi.SupplierInvoiceResponse> listSupplierInvoices() {
        return procurementService.listSupplierInvoices();
    }

    @PostMapping("/invoices")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcurementApi.SupplierInvoiceResponse createSupplierInvoice(
            @Valid @RequestBody ProcurementApi.SupplierInvoicePayload payload) {
        return procurementService.createSupplierInvoice(payload);
    }

    // ─── Supplier Payments ──────────────────────────────────────────

    @GetMapping("/payments")
    public List<ProcurementApi.SupplierPaymentResponse> listSupplierPayments() {
        return procurementService.listSupplierPayments();
    }

    @PostMapping("/payments")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcurementApi.SupplierPaymentResponse createSupplierPayment(
            @Valid @RequestBody ProcurementApi.SupplierPaymentPayload payload) {
        return procurementService.createSupplierPayment(payload);
    }
}
