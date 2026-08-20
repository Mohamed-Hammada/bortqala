package com.bemo.hr.trade.sales.api;

import com.bemo.hr.trade.sales.application.SalesQuotationService;
import com.bemo.hr.trade.sales.domain.QuotationStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trade/sales/quotations")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'VIEWER')")
public class SalesQuotationController {

    private final SalesQuotationService quotationService;

    public SalesQuotationController(SalesQuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @GetMapping
    public ResponseEntity<List<SalesQuotationApi.QuotationResponse>> listQuotations(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) QuotationStatus status) {
        return ResponseEntity.ok(quotationService.listQuotations(customerId, status));
    }

    @PostMapping
    public ResponseEntity<SalesQuotationApi.QuotationResponse> createQuotation(
            @Valid @RequestBody SalesQuotationApi.CreateQuotationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quotationService.createQuotation(request));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<SalesQuotationApi.QuotationResponse> sendQuotation(@PathVariable String id) {
        return ResponseEntity.ok(quotationService.sendQuotation(id));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<SalesQuotationApi.QuotationResponse> acceptQuotation(@PathVariable String id) {
        return ResponseEntity.ok(quotationService.acceptQuotation(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<SalesQuotationApi.QuotationResponse> rejectQuotation(@PathVariable String id) {
        return ResponseEntity.ok(quotationService.rejectQuotation(id));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<SalesQuotationApi.QuotationResponse> convertToSalesOrder(@PathVariable String id) {
        return ResponseEntity.ok(quotationService.convertToSalesOrder(id));
    }
}
