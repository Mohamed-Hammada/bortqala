package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.domain.PurchaseOrder;
import com.bemo.hr.trade.procurement.domain.PurchaseOrderLine;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderLineRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProcurementService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final BusinessPartyRepository businessPartyRepository;
    private final AuditService auditService;

    public ProcurementService(PurchaseOrderRepository purchaseOrderRepository,
                              PurchaseOrderLineRepository purchaseOrderLineRepository,
                              BusinessPartyRepository businessPartyRepository,
                              AuditService auditService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderLineRepository = purchaseOrderLineRepository;
        this.businessPartyRepository = businessPartyRepository;
        this.auditService = auditService;
    }

    public List<ProcurementApi.PurchaseOrderResponse> list() {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAllByOrderByPoDateDescCreatedAtDesc();
        Map<String, String> supplierNames = resolveSupplierNames(orders);
        return orders.stream().map(po -> toResponse(po, loadLines(po.getId()), supplierNames)).toList();
    }

    @Transactional
    public ProcurementApi.PurchaseOrderResponse create(ProcurementApi.PurchaseOrderPayload payload) {
        LocalDate poDate = Instant.ofEpochMilli(payload.poDate()).atZone(ZoneOffset.UTC).toLocalDate();
        PurchaseOrder po = new PurchaseOrder(payload.poNumber(), poDate, payload.supplierId(),
                payload.purchaseRequestId(), payload.paymentTerms(), payload.totalAmount());
        PurchaseOrder saved = purchaseOrderRepository.save(po);

        List<PurchaseOrderLine> lines = payload.items() != null
                ? payload.items().stream()
                    .map(item -> new PurchaseOrderLine(saved.getId(), item.itemName(), item.itemCategory(),
                            item.quantity(), item.unitOfMeasure(), item.unitPrice()))
                    .toList()
                : List.of();
        purchaseOrderLineRepository.saveAll(lines);

        auditService.record("CREATE", "PURCHASE_ORDER", saved.getId(), getCurrentUser(),
                "{\"poNumber\":\"" + saved.getPoNumber() + "\",\"totalAmount\":" + saved.getTotalAmount() + "}", null);
        return toResponse(saved, lines, resolveSupplierNames(List.of(saved)));
    }

    @Transactional
    public ProcurementApi.PurchaseOrderResponse issue(String id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("أمر الشراء غير موجود"));
        po.updateStatus(PurchaseOrder.Status.ISSUED);
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        auditService.record("ISSUE", "PURCHASE_ORDER", saved.getId(), getCurrentUser(),
                "{\"poNumber\":\"" + saved.getPoNumber() + "\"}", null);
        return toResponse(saved, loadLines(saved.getId()), resolveSupplierNames(List.of(saved)));
    }

    private String resolveSupplierName(String supplierId) {
        return businessPartyRepository.findById(supplierId)
                .map(com.bemo.hr.party.BusinessParty::getName)
                .orElse(null);
    }

    private Map<String, String> resolveSupplierNames(List<PurchaseOrder> orders) {
        return orders.stream()
                .map(PurchaseOrder::getSupplierId)
                .distinct()
                .collect(Collectors.toMap(id -> id, this::resolveSupplierName));
    }

    private List<PurchaseOrderLine> loadLines(String purchaseOrderId) {
        return purchaseOrderLineRepository.findByPurchaseOrderId(purchaseOrderId);
    }

    private ProcurementApi.PurchaseOrderResponse toResponse(PurchaseOrder po, List<PurchaseOrderLine> lines,
                                                            Map<String, String> supplierNames) {
        long poDateMs = po.getPoDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return new ProcurementApi.PurchaseOrderResponse(
                po.getId(), po.getPoNumber(), poDateMs, po.getSupplierId(),
                supplierNames.get(po.getSupplierId()),
                po.getPurchaseRequestId(), po.getPaymentTerms(), po.getStatus().name(),
                po.getTotalAmount(),
                lines.stream().map(this::toLineResponse).toList(),
                po.getCreatedAt(), po.getUpdatedAt()
        );
    }

    private ProcurementApi.PurchaseOrderLineResponse toLineResponse(PurchaseOrderLine line) {
        return new ProcurementApi.PurchaseOrderLineResponse(
                line.getId(), line.getItemName(), line.getItemCategory(),
                line.getQuantity(), line.getUnitOfMeasure(), line.getUnitPrice(), line.getLineTotal()
        );
    }

    private String getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName() : "system";
    }
}
