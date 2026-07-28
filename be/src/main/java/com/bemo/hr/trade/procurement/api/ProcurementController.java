package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.trade.procurement.domain.PurchaseOrder;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trade/procurement")
public class ProcurementController {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final com.bemo.hr.audit.application.AuditService auditService;

    public ProcurementController(PurchaseOrderRepository purchaseOrderRepository, com.bemo.hr.audit.application.AuditService auditService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.auditService = auditService;
    }

    @GetMapping("/orders")
    public List<ProcurementApi.PurchaseOrderResponse> listPurchaseOrders() {
        return purchaseOrderRepository.findAllByOrderByPoDateDescCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @PostMapping("/orders")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public ProcurementApi.PurchaseOrderResponse createPurchaseOrder(@Valid @RequestBody ProcurementApi.PurchaseOrderPayload payload) {
        LocalDate poDate = Instant.ofEpochMilli(payload.poDate()).atZone(ZoneOffset.UTC).toLocalDate();
        PurchaseOrder po = new PurchaseOrder(payload.poNumber(), poDate, payload.supplierId(), payload.purchaseRequestId(), payload.paymentTerms(), payload.totalAmount());
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        auditService.record("CREATE", "PURCHASE_ORDER", saved.getId(), getCurrentUser(),
                "{\"poNumber\":\"" + saved.getPoNumber() + "\",\"totalAmount\":" + saved.getTotalAmount() + "}", null);
        return toResponse(saved);
    }

    @PostMapping("/orders/{id}/issue")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public ProcurementApi.PurchaseOrderResponse issuePurchaseOrder(@PathVariable String id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("أمر الشراء غير موجود"));
        po.updateStatus(PurchaseOrder.Status.ISSUED);
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        auditService.record("ISSUE", "PURCHASE_ORDER", saved.getId(), getCurrentUser(),
                "{\"poNumber\":\"" + saved.getPoNumber() + "\"}", null);
        return toResponse(saved);
    }

    private String getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName() : "system";
    }

    private ProcurementApi.PurchaseOrderResponse toResponse(PurchaseOrder po) {
        long poDateMs = po.getPoDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return new ProcurementApi.PurchaseOrderResponse(
                po.getId(), po.getPoNumber(), poDateMs, po.getSupplierId(),
                po.getPurchaseRequestId(), po.getPaymentTerms(), po.getStatus().name(),
                po.getTotalAmount(), po.getCreatedAt(), po.getUpdatedAt()
        );
    }
}
