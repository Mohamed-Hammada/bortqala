package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.budget.application.BudgetService;
import com.bemo.hr.finance.domain.Currency;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.finance.infrastructure.CurrencyRepository;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.operations.PartnerLedgerEntry;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.idempotency.application.IdempotencyService;
import com.bemo.hr.shared.numbering.DocumentNumberService;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.domain.*;
import com.bemo.hr.trade.procurement.infrastructure.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ProcurementService {

    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)$");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final ProcurementDocumentSequenceRepository procurementDocumentSequenceRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierReturnRepository supplierReturnRepository;
    private final BusinessPartyRepository businessPartyRepository;
    private final PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    private final AuditService auditService;
    private final ProcurementExcelExporter procurementExcelExporter;
    private final OperationsService operationsService;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final CurrencyRepository currencyRepository;
    private final IdempotencyService idempotencyService;
    private final FiscalPeriodGuard fiscalPeriodGuard;
    private final DocumentNumberService documentNumberService;
    private final com.bemo.hr.trade.procurement.domain.ProcurementThreeWayMatchRepository threeWayMatchRepository;
    private final BudgetService budgetService;
    private final ProcurementAccountingService procurementAccountingService;

    public ProcurementService(PurchaseOrderRepository purchaseOrderRepository,
                              PurchaseOrderLineRepository purchaseOrderLineRepository,
                              ProcurementDocumentSequenceRepository procurementDocumentSequenceRepository,
                              GoodsReceiptRepository goodsReceiptRepository,
                              SupplierInvoiceRepository supplierInvoiceRepository,
                              SupplierPaymentRepository supplierPaymentRepository,
                              SupplierReturnRepository supplierReturnRepository,
                              BusinessPartyRepository businessPartyRepository,
                              PartnerLedgerEntryRepository partnerLedgerEntryRepository,
                              AuditService auditService,
                              ProcurementExcelExporter procurementExcelExporter,
                              OperationsService operationsService,
                              TenantApplicationRepository tenantApplicationRepository,
                              CurrencyRepository currencyRepository,
                              IdempotencyService idempotencyService,
                              FiscalPeriodGuard fiscalPeriodGuard,
                              DocumentNumberService documentNumberService,
                              com.bemo.hr.trade.procurement.domain.ProcurementThreeWayMatchRepository threeWayMatchRepository,
                              BudgetService budgetService,
                              ProcurementAccountingService procurementAccountingService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderLineRepository = purchaseOrderLineRepository;
        this.procurementDocumentSequenceRepository = procurementDocumentSequenceRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.supplierReturnRepository = supplierReturnRepository;
        this.businessPartyRepository = businessPartyRepository;
        this.partnerLedgerEntryRepository = partnerLedgerEntryRepository;
        this.auditService = auditService;
        this.procurementExcelExporter = procurementExcelExporter;
        this.operationsService = operationsService;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.currencyRepository = currencyRepository;
        this.idempotencyService = idempotencyService;
        this.fiscalPeriodGuard = fiscalPeriodGuard;
        this.documentNumberService = documentNumberService;
        this.threeWayMatchRepository = threeWayMatchRepository;
        this.budgetService = budgetService;
        this.procurementAccountingService = procurementAccountingService;
    }

    public ProcurementApi.NumberingSettings numberingSettings() {
        return new ProcurementApi.NumberingSettings(automaticNumbering());
    }

    public byte[] export(String locale, String actor) {
        return procurementExcelExporter.export(list(), listGoodsReceipts(), listSupplierInvoices(), listSupplierPayments(), locale, actor);
    }

    // ─── Purchase Orders ──────────────────────────────────────────────

    public List<ProcurementApi.PurchaseOrderResponse> list() {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAllByOrderByPoDateDescCreatedAtDesc();
        Map<String, String> supplierNames = resolveSupplierNames(orders);
        return orders.stream().map(po -> toPoResponse(po, loadLines(po.getId()), supplierNames)).toList();
    }

    @Transactional
    public ProcurementApi.PurchaseOrderResponse create(ProcurementApi.PurchaseOrderPayload payload) {
        log.debug("create PO called with supplierId={}, currencyCode={}", payload.supplierId(), payload.currencyCode());
        requireSupplier(payload.supplierId());
        validateLines(payload.items());
        String currencyCode = resolveCurrency(payload.currencyCode(), payload.supplierId());
        BigDecimal calculatedTotal = payload.items().stream()
                .map(item -> item.quantity().multiply(item.unitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate poDate = Instant.ofEpochMilli(payload.poDate()).atZone(ZoneOffset.UTC).toLocalDate();
        ExchangeRateSnapshot rate = resolveExchangeRate(currencyCode, poDate, payload.exchangeRate(),
                payload.exchangeRateOverrideReason());
        PurchaseOrder po = new PurchaseOrder(resolvePoNumber(payload.poNumber(), null), poDate, payload.supplierId(),
                payload.purchaseRequestId(), payload.paymentTerms(), currencyCode, calculatedTotal);
        po.assignDepartment(payload.departmentId());
        po.applyExchangeRate(rate.baseCurrencyCode(), rate.rate(), rate.rateDate(), rate.source(), rate.overrideReason());
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        List<PurchaseOrderLine> lines = buildLines(saved.getId(), payload.items());
        purchaseOrderLineRepository.saveAll(lines);

        auditService.record("CREATE", "PURCHASE_ORDER", saved.getId(), getCurrentUser(),
                "{\"poNumber\":\"" + saved.getPoNumber() + "\",\"totalAmount\":" + saved.getTotalAmount() + "}", null);
        log.info("PurchaseOrder {} created with number {} successfully", saved.getId(), saved.getPoNumber());
        return toPoResponse(saved, lines, resolveSupplierNames(List.of(saved)));
    }

    @Transactional
    public ProcurementApi.PurchaseOrderResponse update(String id, ProcurementApi.PurchaseOrderPayload payload) {
        log.debug("update PO called with id={}", id);
        PurchaseOrder po = requirePo(id);
        if (po.getStatus() != PurchaseOrder.Status.DRAFT) {
            log.warn("Validation failed: PurchaseOrder {} is not DRAFT, status={}", id, po.getStatus());
            throw new BusinessRuleException("Only draft purchase orders can be edited.", "PROC_ORDER_NOT_DRAFT", HttpStatus.CONFLICT);
        }
        requireSupplier(payload.supplierId());
        validateLines(payload.items());
        String currencyCode = po.getCurrencyCode();
        BigDecimal calculatedTotal = payload.items().stream()
                .map(item -> item.quantity().multiply(item.unitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate poDate = Instant.ofEpochMilli(payload.poDate()).atZone(ZoneOffset.UTC).toLocalDate();
        validateFrozenPurchaseOrderExchangeSnapshot(po, payload, poDate);
        po.updateDraft(resolvePoNumber(payload.poNumber(), po.getId()), poDate, payload.supplierId(),
                payload.purchaseRequestId(), payload.paymentTerms(), currencyCode, calculatedTotal);
        po.assignDepartment(payload.departmentId());
        po.applyExchangeRate(po.getBaseCurrencyCode(), po.getExchangeRate(), po.getExchangeRateDate(),
                po.getExchangeRateSource(), po.getExchangeRateOverrideReason());
        purchaseOrderLineRepository.deleteByPurchaseOrderId(po.getId());
        List<PurchaseOrderLine> lines = buildLines(po.getId(), payload.items());
        purchaseOrderLineRepository.saveAll(lines);
        auditService.record("UPDATE", "PURCHASE_ORDER", po.getId(), getCurrentUser(),
                "{\"poNumber\":\"" + po.getPoNumber() + "\",\"totalAmount\":" + po.getTotalAmount() + "}", null);
        log.info("PurchaseOrder {} updated with number {} successfully", id, po.getPoNumber());
        return toPoResponse(po, lines, resolveSupplierNames(List.of(po)));
    }

    @Transactional
    public ProcurementApi.PurchaseOrderResponse issue(String id) {
        log.debug("issue PO called with id={}", id);
        PurchaseOrder po = requirePo(id);
        if (po.getStatus() != PurchaseOrder.Status.DRAFT) {
            log.warn("Validation failed: PurchaseOrder {} cannot be issued from status {}", id, po.getStatus());
            throw new BusinessRuleException("يمكن إصدار أمر الشراء من حالة مسودة فقط", "PROC_ORDER_ISSUE_FROM_DRAFT", HttpStatus.CONFLICT);
        }
        po.updateStatus(PurchaseOrder.Status.ISSUED);
        budgetService.encumberForOrder(po.getId(), po.getPoNumber(), po.getDepartmentId(),
                po.getBaseTotalAmount(), po.getPoDate(), getCurrentUser());
        auditService.record("ISSUE", "PURCHASE_ORDER", po.getId(), getCurrentUser(),
                "{\"poNumber\":\"" + po.getPoNumber() + "\"}", null);
        log.info("PurchaseOrder {} issued successfully", id);
        return toPoResponse(po, loadLines(po.getId()), resolveSupplierNames(List.of(po)));
    }

    @Transactional
    public ProcurementApi.PurchaseOrderResponse receive(String id) {
        log.debug("receive PO called with id={} (deprecated)", id);
        throw new BusinessRuleException("يجب تسجيل إذن استلام بضاعة (Goods Receipt) لاستلام أمر الشراء.", "PROC_DIRECT_RECEIVE_DEPRECATED", HttpStatus.BAD_REQUEST);
    }

    @Transactional
    public ProcurementApi.PurchaseOrderResponse cancel(String id) {
        log.debug("cancel PO called with id={}", id);
        PurchaseOrder po = requirePo(id);
        if (po.getStatus() == PurchaseOrder.Status.CANCELLED) {
            log.warn("Validation failed: PurchaseOrder {} is already cancelled", id);
            throw new BusinessRuleException("أمر الشراء ملغي بالفعل", "PROC_ORDER_ALREADY_CANCELLED", HttpStatus.CONFLICT);
        }

        List<GoodsReceipt> grns = goodsReceiptRepository.findByPurchaseOrderId(po.getId());
        BigDecimal totalAccepted = grns.stream()
                .flatMap(g -> g.getLines().stream())
                .map(GoodsReceiptLine::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAccepted.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("لا يمكن إلغاء أمر شراء تم استلام بضائع عليه. يجب استخدام إذن إرجاع للمورد.", "PO_HAS_RECEIPTS_CANNOT_CANCEL", HttpStatus.CONFLICT);
        }

        List<SupplierInvoice> invoices = supplierInvoiceRepository.findByPurchaseOrderId(po.getId());
        if (!invoices.isEmpty()) {
            throw new BusinessRuleException("لا يمكن إلغاء أمر شراء له فواتير مورّدين.", "PO_HAS_INVOICES_CANNOT_CANCEL", HttpStatus.CONFLICT);
        }

        po.updateStatus(PurchaseOrder.Status.CANCELLED);
        budgetService.releaseForCancel(po.getId(), getCurrentUser());
        auditService.record("CANCEL", "PURCHASE_ORDER", po.getId(), getCurrentUser(),
                "{\"poNumber\":\"" + po.getPoNumber() + "\"}", null);
        log.info("PurchaseOrder {} cancelled successfully", id);
        return toPoResponse(po, loadLines(po.getId()), resolveSupplierNames(List.of(po)));
    }

    // ─── Goods Receipts ───────────────────────────────────────────────

    public List<ProcurementApi.GoodsReceiptResponse> listGoodsReceipts() {
        log.debug("listGoodsReceipts called");
        List<GoodsReceipt> grns = goodsReceiptRepository.findAllByOrderByReceiptDateDesc();
        Map<String, String> supplierNames = resolveNames(grns.stream().map(GoodsReceipt::getSupplierId).distinct().toList());
        return grns.stream().map(grn -> toGrnResponse(grn, supplierNames)).toList();
    }

    @Transactional
    public ProcurementApi.GoodsReceiptResponse createGoodsReceipt(ProcurementApi.GoodsReceiptPayload payload) {
        log.debug("createGoodsReceipt called with purchaseOrderId={}, supplierId={}", payload.purchaseOrderId(), payload.supplierId());
        PurchaseOrder po = requirePo(payload.purchaseOrderId());
        if (po.getStatus() != PurchaseOrder.Status.ISSUED
                && po.getStatus() != PurchaseOrder.Status.PARTIALLY_RECEIVED)
            throw new BusinessRuleException("يمكن إضافة إذن استلام فقط لأوامر الشراء المفتوحة", "PROC_GRN_OPEN_ORDER_ONLY", HttpStatus.CONFLICT);

        if (!po.getSupplierId().equals(payload.supplierId()))
            throw new BusinessRuleException("Goods receipt supplier must match the purchase order supplier.", "PROC_GRN_SUPPLIER_MISMATCH", HttpStatus.CONFLICT);
        if (payload.lines() == null || payload.lines().isEmpty())
            throw new BusinessRuleException("Goods receipt requires at least one line.", "PROC_GRN_LINE_REQUIRED", HttpStatus.CONFLICT);

        Map<String, PurchaseOrderLine> orderedLines = loadLines(po.getId()).stream()
                .collect(Collectors.toMap(PurchaseOrderLine::getId, line -> line));
        Map<String, BigDecimal> previouslyAccepted = new HashMap<>();
        goodsReceiptRepository.findByPurchaseOrderId(po.getId()).forEach(receipt -> receipt.getLines().forEach(line ->
                previouslyAccepted.merge(line.getPurchaseOrderLineId(), line.getQuantity(), BigDecimal::add)));

        LocalDate receiptDate = Instant.ofEpochMilli(payload.receiptDate()).atZone(ZoneOffset.UTC).toLocalDate();
        List<GoodsReceiptLine> lines = payload.lines().stream().map(line -> {
            PurchaseOrderLine ordered = orderedLines.get(line.purchaseOrderLineId());
            if (ordered == null)
                throw new BusinessRuleException("Goods receipt line does not belong to the selected purchase order.", "PROC_GRN_LINE_WRONG_ORDER", HttpStatus.CONFLICT);
            if (ordered.getItemId() == null || !ordered.getItemId().equals(line.itemId()))
                throw new BusinessRuleException("Goods receipt inventory item must match its purchase-order line.", "PROC_GRN_ITEM_MISMATCH", HttpStatus.CONFLICT);
            BigDecimal delivered = line.deliveredQuantity();
            BigDecimal rejected = line.rejectedQuantity();
            BigDecimal deducted = line.deductedQuantity();
            if (delivered == null || delivered.signum() <= 0 || rejected.signum() < 0 || deducted.signum() < 0)
                throw new BusinessRuleException("الكمية المستلمة يجب أن تكون أكبر من صفر، ولا يمكن أن تكون المرفوضة أو المخصومة سالبة.", "PROC_GRN_QUANTITY_POSITIVE", HttpStatus.CONFLICT);
            BigDecimal accepted = delivered.subtract(rejected).subtract(deducted);
            if (accepted.signum() < 0)
                throw new BusinessRuleException("مجموع الكمية المرفوضة والمخصومة لا يمكن أن يتجاوز الكمية المستلمة.", "PROC_GRN_REJECTED_EXCEEDS", HttpStatus.CONFLICT);
            BigDecimal remaining = ordered.getQuantity().subtract(previouslyAccepted.getOrDefault(ordered.getId(), BigDecimal.ZERO));
            if (accepted.compareTo(remaining) > 0)
                throw new BusinessRuleException("الكمية المقبولة تتجاوز المتبقي في أمر الشراء للصنف: " + ordered.getItemName());
            previouslyAccepted.merge(ordered.getId(), accepted, BigDecimal::add);
            return new GoodsReceiptLine(line.purchaseOrderLineId(), line.itemId(), ordered.getItemName(),
                    ordered.getItemCategory(), delivered, rejected, deducted, accepted, ordered.getUnitOfMeasure(),
                    ordered.getUnitPrice(), normalizeOptional(line.locationId()), normalizeOptional(line.lotNumber()),
                    normalizeOptional(line.qualityReason()));
        }).toList();
        if (lines.stream().map(GoodsReceiptLine::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add).signum() <= 0)
            throw new BusinessRuleException("Goods receipt must contain an accepted quantity.", "PROC_GRN_ACCEPTED_QUANTITY_REQUIRED", HttpStatus.CONFLICT);

        GoodsReceipt grn = new GoodsReceipt(resolveGrnNumber(payload.grnNumber()), receiptDate, payload.purchaseOrderId(),
                payload.supplierId(), payload.warehouseId(), payload.notes(), lines);
        GoodsReceipt saved = goodsReceiptRepository.save(grn);

        String actor = getCurrentUser();
        lines.stream().filter(line -> line.getQuantity().signum() > 0).forEach(line ->
                operationsService.recordGoodsReceipt(line.getItemId(), po.getSupplierId(), payload.warehouseId(), line.getQuantity(), line.getUnitPrice(),
                        saved.getGrnNumber(), line.getLotNumber(), line.getQualityReason(),
                        receiptDate.atStartOfDay(ZoneOffset.UTC).toInstant(), actor));
        boolean fullyReceived = orderedLines.values().stream().allMatch(line ->
                previouslyAccepted.getOrDefault(line.getId(), BigDecimal.ZERO).compareTo(line.getQuantity()) >= 0);
        po.updateStatus(fullyReceived ? PurchaseOrder.Status.RECEIVED : PurchaseOrder.Status.PARTIALLY_RECEIVED);
        if (fullyReceived) budgetService.releaseForReceive(po.getId(), actor);

        auditService.record("CREATE", "GOODS_RECEIPT", saved.getId(), getCurrentUser(),
                "{\"grnNumber\":\"" + saved.getGrnNumber() + "\",\"po\":\"" + po.getPoNumber() + "\"}", null);
        log.info("GoodsReceipt {} created with number {} for PO {} successfully", saved.getId(), saved.getGrnNumber(), po.getPoNumber());
        return toGrnResponse(saved, resolveNames(List.of(saved.getSupplierId())));
    }

    // ─── Supplier Invoices ────────────────────────────────────────────

    public List<ProcurementApi.SupplierInvoiceResponse> listSupplierInvoices() {
        log.debug("listSupplierInvoices called");
        List<SupplierInvoice> invoices = supplierInvoiceRepository.findAllByOrderByInvoiceDateDesc();
        Map<String, String> supplierNames = resolveNames(invoices.stream().map(SupplierInvoice::getSupplierId).distinct().toList());
        return invoices.stream().map(inv -> toInvoiceResponse(inv, supplierNames)).toList();
    }

    @Transactional
    public ProcurementApi.SupplierInvoiceResponse createSupplierInvoice(ProcurementApi.SupplierInvoicePayload payload) {
        log.debug("createSupplierInvoice called with supplierId={}, purchaseOrderId={}", payload.supplierId(), payload.purchaseOrderId());
        var supplier = businessPartyRepository.findById(payload.supplierId())
                .orElseThrow(() -> new BusinessRuleException("Supplier not found.", "PROC_SUPPLIER_NOT_FOUND", HttpStatus.CONFLICT));
        boolean missingInvoice = payload.invoiceNumber() == null || payload.invoiceNumber().isBlank();
        if ("DIRECT".equals(supplier.getManagedType()) && missingInvoice)
            throw new BusinessRuleException("A direct supplier requires a supplier invoice number.", "PROC_DIRECT_SUPPLIER_INVOICE_REQUIRED", HttpStatus.CONFLICT);
        if (missingInvoice && ((payload.internalReference() == null || payload.internalReference().isBlank())
                || payload.missingInvoiceReason() == null || payload.missingInvoiceReason().isBlank()))
            throw new BusinessRuleException("Managed supplier transactions without an invoice require an internal reference and reason.", "PROC_MANAGED_SUPPLIER_REFERENCE_REQUIRED", HttpStatus.CONFLICT);
        String internalReference = payload.internalReference() == null || payload.internalReference().isBlank()
                ? payload.invoiceNumber().strip() : payload.internalReference().strip();
        String currencyCode = resolveCurrency(payload.currencyCode(), payload.supplierId());
        BigDecimal discount = payload.discountAmount() == null ? BigDecimal.ZERO : payload.discountAmount();
        BigDecimal tax = payload.taxAmount() == null ? BigDecimal.ZERO : payload.taxAmount();
        if (payload.totalAmount().signum() <= 0 || discount.signum() < 0 || tax.signum() < 0
                || discount.compareTo(payload.totalAmount()) > 0)
            throw new BusinessRuleException("Invoice amounts are invalid.", "PROC_INVOICE_AMOUNTS_INVALID", HttpStatus.CONFLICT);
        if (payload.purchaseOrderId() != null) {
            PurchaseOrder po = requirePo(payload.purchaseOrderId());
            if (po.getStatus() == PurchaseOrder.Status.CANCELLED)
                throw new BusinessRuleException("لا يمكن إصدار فاتورة لأمر شراء ملغي", "PROC_INVOICE_CANCELLED_ORDER", HttpStatus.CONFLICT);
        }

        LocalDate invoiceDate = Instant.ofEpochMilli(payload.invoiceDate()).atZone(ZoneOffset.UTC).toLocalDate();
        fiscalPeriodGuard.requireOpen(invoiceDate);
        ExchangeRateSnapshot rate = resolveExchangeRate(currencyCode, invoiceDate, payload.exchangeRate(),
                payload.exchangeRateOverrideReason());
        LocalDate dueDate = payload.dueDate() != null
                ? Instant.ofEpochMilli(payload.dueDate()).atZone(ZoneOffset.UTC).toLocalDate()
                : null;

        String invoiceNotes = missingInvoice ? payload.missingInvoiceReason() + (payload.notes() == null ? "" : " — " + payload.notes()) : payload.notes();
        SupplierInvoice inv = new SupplierInvoice(missingInvoice ? null : payload.invoiceNumber(), internalReference,
                missingInvoice ? payload.missingInvoiceReason() : null, currencyCode, payload.supplierId(),
                payload.purchaseOrderId(), payload.goodsReceiptId(), supplier.getResponsiblePartyId(),
                invoiceDate, payload.totalAmount(),
                payload.discountAmount(), payload.taxAmount(), dueDate, invoiceNotes);
        inv.applyExchangeRate(rate.baseCurrencyCode(), rate.rate(), rate.rateDate(), rate.source(), rate.overrideReason());
        SupplierInvoice saved = supplierInvoiceRepository.save(inv);

        partnerLedgerEntryRepository.save(new PartnerLedgerEntry(
                saved.getSupplierId(), "PURCHASE_INVOICE", saved.getBaseNetAmount().negate(),
                saved.getDocumentReference(), "فاتورة مشتريات: " + saved.getDocumentReference(),
                saved.getInvoiceDate().atStartOfDay(ZoneOffset.UTC).toInstant(), getCurrentUser()));

        procurementAccountingService.postSupplierInvoice(saved, getCurrentUser());
        budgetService.liquidateForInvoice(saved.getPurchaseOrderId(), saved.getBaseNetAmount(), getCurrentUser());
        auditService.record("CREATE", "SUPPLIER_INVOICE", saved.getId(), getCurrentUser(),
                "{\"invoiceNumber\":\"" + saved.getDocumentReference() + "\",\"amount\":" + saved.getNetAmount() + "}", null);
        log.info("SupplierInvoice {} created with reference {} successfully", saved.getId(), saved.getDocumentReference());
        return toInvoiceResponse(saved, resolveNames(List.of(saved.getSupplierId())));
    }

    // ─── Supplier Payments ────────────────────────────────────────────

    public List<ProcurementApi.SupplierPaymentResponse> listSupplierPayments() {
        log.debug("listSupplierPayments called");
        List<SupplierPayment> payments = supplierPaymentRepository.findAllByOrderByPaymentDateDesc();
        Map<String, String> supplierNames = resolveNames(payments.stream().map(SupplierPayment::getSupplierId).distinct().toList());
        return payments.stream().map(pmt -> toPaymentResponse(pmt, supplierNames)).toList();
    }

    @Transactional
    public ProcurementApi.SupplierPaymentResponse createSupplierPayment(ProcurementApi.SupplierPaymentPayload payload) {
        log.debug("createSupplierPayment called with supplierId={}, supplierInvoiceId={}, amount={}", payload.supplierId(), payload.supplierInvoiceId(), payload.amount());
        String requestHash = IdempotencyService.hash(payload.supplierId() + "|" + payload.supplierInvoiceId()
                + "|" + payload.amount().stripTrailingZeros().toPlainString() + "|" + payload.paymentDate());
        return idempotencyService.execute("SUPPLIER_PAYMENT", payload.operationId(), requestHash,
                () -> createSupplierPaymentTransaction(payload),
                payment -> payment.id(),
                this::replayPayment);
    }

    /**
     * Creates every allocation of one locked payment proposal in the caller's transaction.
     * Proposal execution owns the operation-id replay guard, so these child payments must not
     * independently complete idempotency reservations before the proposal transaction commits.
     */
    @Transactional
    public List<ProcurementApi.SupplierPaymentResponse> createSupplierPaymentsForProposal(
            List<ProcurementApi.SupplierPaymentPayload> allocations) {
        return allocations.stream().map(this::createSupplierPaymentTransaction).toList();
    }

    private ProcurementApi.SupplierPaymentResponse replayPayment(String paymentId) {
        SupplierPayment payment = supplierPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessRuleException("الدفعة غير موجودة.", "PROC_PAYMENT_NOT_FOUND", HttpStatus.CONFLICT));
        return toPaymentResponse(payment, resolveNames(List.of(payment.getSupplierId())));
    }

    private ProcurementApi.SupplierPaymentResponse createSupplierPaymentTransaction(ProcurementApi.SupplierPaymentPayload payload) {
        var replay = supplierPaymentRepository.findByOperationId(payload.operationId());
        if (replay.isPresent()) {
            SupplierPayment existing = replay.get();
            if (!existing.getSupplierId().equals(payload.supplierId())
                    || !existing.getSupplierInvoiceId().equals(payload.supplierInvoiceId())
                    || existing.getAmount().compareTo(payload.amount()) != 0) {
                throw new BusinessRuleException("معرّف عملية الدفع مستخدم لدفعة مختلفة.", "PROC_PAYMENT_OPERATION_CONFLICT", HttpStatus.CONFLICT);
            }
            return toPaymentResponse(existing, resolveNames(List.of(existing.getSupplierId())));
        }
        SupplierInvoice inv = supplierInvoiceRepository.findByIdForPayment(payload.supplierInvoiceId())
                .orElseThrow(() -> new BusinessRuleException("الفاتورة غير موجودة", "PROC_INVOICE_NOT_FOUND", HttpStatus.CONFLICT));
        if ("PAID".equals(inv.getStatus()) || "CANCELLED".equals(inv.getStatus()))
            throw new BusinessRuleException("الفاتورة مدفوعة بالفعل أو ملغية", "PROC_INVOICE_ALREADY_PAID", HttpStatus.CONFLICT);

        if (!inv.getSupplierId().equals(payload.supplierId()))
            throw new BusinessRuleException("الفاتورة المحددة لا تخص المورد المختار. اختر فاتورة مفتوحة لنفس المورد.", "PROC_INVOICE_SUPPLIER_MISMATCH", HttpStatus.CONFLICT);
        requirePayableSupplier(payload.supplierId());
        if (payload.amount().signum() <= 0)
            throw new BusinessRuleException("يجب أن يكون مبلغ الدفعة أكبر من صفر.", "PROC_PAYMENT_AMOUNT_POSITIVE", HttpStatus.CONFLICT);
        BigDecimal paidBefore = paidAmount(inv.getId());
        BigDecimal outstanding = inv.getNetAmount().subtract(paidBefore);
        if (payload.amount().compareTo(outstanding) > 0)
            throw new BusinessRuleException("مبلغ الدفعة يتجاوز الرصيد المتبقي للفاتورة وهو " + outstanding + " " + inv.getCurrencyCode() + ".");

        LocalDate paymentDate = Instant.ofEpochMilli(payload.paymentDate()).atZone(ZoneOffset.UTC).toLocalDate();
        fiscalPeriodGuard.requireOpen(paymentDate);
        SupplierPayment pmt = new SupplierPayment(resolvePaymentNumber(payload, paymentDate), paymentDate, payload.supplierId(),
                payload.supplierInvoiceId(), payload.operationId(), payload.amount(), payload.paymentMethod(), payload.notes());
        pmt.freezeBeneficiaryBankAccount(businessPartyRepository.findById(payload.supplierId())
                .map(com.bemo.hr.party.BusinessParty::getBankAccount).orElse(null));
        SupplierPayment saved = supplierPaymentRepository.save(pmt);

        inv.updatePaymentStatus(paidBefore.add(saved.getAmount()));

        partnerLedgerEntryRepository.save(new PartnerLedgerEntry(
                saved.getSupplierId(), "SUPPLIER_PAYMENT",
                saved.getAmount().multiply(inv.getExchangeRate()).setScale(2, java.math.RoundingMode.HALF_UP),
                saved.getPaymentNumber(), "دفعة مورد: " + saved.getPaymentNumber(),
                saved.getPaymentDate().atStartOfDay(ZoneOffset.UTC).toInstant(), getCurrentUser()));

        procurementAccountingService.postSupplierPayment(saved, inv, getCurrentUser());
        auditService.record("CREATE", "SUPPLIER_PAYMENT", saved.getId(), getCurrentUser(),
                "{\"paymentNumber\":\"" + saved.getPaymentNumber() + "\",\"operationId\":\""
                        + saved.getOperationId() + "\",\"amount\":" + saved.getAmount() + "}", null);
        log.info("SupplierPayment {} created with number {} successfully", saved.getId(), saved.getPaymentNumber());
        return toPaymentResponse(saved, resolveNames(List.of(saved.getSupplierId())));
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private PurchaseOrder requirePo(String id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("أمر الشراء غير موجود", "PROC_ORDER_NOT_FOUND", HttpStatus.CONFLICT));
    }

    private void requireSupplier(String id) {
        var supplier = businessPartyRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("المورد غير موجود في دليل الموردين.", "PROC_SUPPLIER_NOT_IN_DIRECTORY", HttpStatus.CONFLICT));
        if (!supplier.isProcurementAllowed())
            throw new BusinessRuleException("يجب اختيار مورد نشط ومسجل في دليل الموردين.", "PROC_SUPPLIER_ACTIVE_REQUIRED", HttpStatus.CONFLICT);
    }

    private void requirePayableSupplier(String id) {
        var supplier = businessPartyRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("المورد غير موجود في دليل الموردين.", "PROC_SUPPLIER_NOT_IN_DIRECTORY", HttpStatus.CONFLICT));
        if (!supplier.isPaymentAllowed()) {
            throw new BusinessRuleException("يجب تفعيل المورد والتحقق من حسابه البنكي قبل الدفع.",
                    "PROC_SUPPLIER_BANK_VERIFICATION_REQUIRED", HttpStatus.CONFLICT);
        }
    }

    private String resolveCurrency(String requested, String supplierId) {
        String supplierCurrency = businessPartyRepository.findById(supplierId)
                .map(com.bemo.hr.party.BusinessParty::getCurrencyCode)
                .orElse("EGP");
        String code = requested == null || requested.isBlank() ? supplierCurrency : requested;
        if (code == null || code.isBlank()) code = "EGP";
        String normalized = code.strip().toUpperCase(java.util.Locale.ROOT);
        currencyRepository.findByCodeIgnoreCaseAndActiveTrue(normalized)
                .orElseThrow(() -> new BusinessRuleException("يجب اختيار عملة نشطة ومسجلة في إعدادات العملات.", "PROC_CURRENCY_ACTIVE_REQUIRED", HttpStatus.CONFLICT));
        return normalized;
    }

    public ProcurementApi.ExchangeRateQuote exchangeRateQuote(String currencyCode, long documentDate) {
        LocalDate date = Instant.ofEpochMilli(documentDate).atZone(ZoneOffset.UTC).toLocalDate();
        ExchangeRateSnapshot snapshot = resolveExchangeRate(currencyCode, date, null, null);
        return new ProcurementApi.ExchangeRateQuote(currencyCode.strip().toUpperCase(java.util.Locale.ROOT),
                snapshot.baseCurrencyCode(), snapshot.rate(), toEpochMs(snapshot.rateDate()), snapshot.source());
    }

    private ExchangeRateSnapshot resolveExchangeRate(String currencyCode, LocalDate documentDate,
                                                     BigDecimal requestedRate, String overrideReason) {
        Currency currency = currencyRepository.findByCodeIgnoreCaseAndActiveTrue(currencyCode)
                .orElseThrow(() -> new BusinessRuleException("لا يوجد سعر صرف نشط للعملة المحددة.", "PROC_EXCHANGE_RATE_MISSING", HttpStatus.CONFLICT));
        String baseCurrency = currencyRepository.findAllByOrderByCodeAsc().stream()
                .filter(item -> item.isActive() && item.isBase()).map(Currency::getCode).findFirst().orElse("EGP");
        BigDecimal configuredRate = currency.getCode().equalsIgnoreCase(baseCurrency)
                ? BigDecimal.ONE : currency.getExchangeRate();
        if (configuredRate == null || configuredRate.signum() <= 0) {
            throw new BusinessRuleException("لا يمكن ترحيل المستند: لا يتوفر سعر صرف صالح للعملة " + currency.getCode() + ".");
        }
        BigDecimal effectiveRate = requestedRate == null ? configuredRate : requestedRate;
        if (effectiveRate.signum() <= 0)
            throw new BusinessRuleException("سعر الصرف يجب أن يكون أكبر من صفر.", "PROC_EXCHANGE_RATE_POSITIVE", HttpStatus.CONFLICT);
        boolean manuallyOverridden = effectiveRate.compareTo(configuredRate) != 0;
        if (manuallyOverridden && (overrideReason == null || overrideReason.isBlank())) {
            throw new BusinessRuleException("اكتب سبب تعديل سعر الصرف يدوياً.", "PROC_EXCHANGE_RATE_REASON_REQUIRED", HttpStatus.CONFLICT);
        }
        String source = manuallyOverridden ? "MANUAL_OVERRIDE"
                : currency.getCode().equalsIgnoreCase(baseCurrency) ? "BASE_CURRENCY" : "CURRENCY_MASTER";
        return new ExchangeRateSnapshot(baseCurrency, effectiveRate, documentDate, source,
                manuallyOverridden ? overrideReason.strip() : null);
    }

    private void validateFrozenPurchaseOrderExchangeSnapshot(PurchaseOrder po,
                                                             ProcurementApi.PurchaseOrderPayload payload, LocalDate requestedDocumentDate) {
        if (!po.getPoDate().equals(requestedDocumentDate)) {
            throw new BusinessRuleException("لا يمكن تغيير تاريخ أمر الشراء بعد حفظ لقطة سعر الصرف.", "PROC_EXCHANGE_RATE_DATE_FROZEN", HttpStatus.CONFLICT);
        }
        if (payload.currencyCode() != null && !payload.currencyCode().isBlank()
                && !po.getCurrencyCode().equalsIgnoreCase(payload.currencyCode().strip())) {
            throw new BusinessRuleException("لا يمكن تغيير عملة أمر الشراء بعد الحفظ؛ أنشئ مستنداً جديداً.", "PROC_EXCHANGE_RATE_CURRENCY_FROZEN", HttpStatus.CONFLICT);
        }
        if (payload.exchangeRate() != null && payload.exchangeRate().compareTo(po.getExchangeRate()) != 0) {
            throw new BusinessRuleException("سعر الصرف مثبت مع أمر الشراء ولا يمكن تغييره بعد الحفظ.", "PROC_EXCHANGE_RATE_FROZEN", HttpStatus.CONFLICT);
        }
        if (payload.exchangeRateOverrideReason() != null && !payload.exchangeRateOverrideReason().isBlank()
                && !java.util.Objects.equals(po.getExchangeRateOverrideReason(), payload.exchangeRateOverrideReason().strip())) {
            throw new BusinessRuleException("سبب تعديل سعر الصرف جزء من اللقطة المثبتة ولا يمكن تغييره.", "PROC_EXCHANGE_RATE_REASON_FROZEN", HttpStatus.CONFLICT);
        }
    }

    private void validateLines(List<ProcurementApi.PurchaseOrderLinePayload> items) {
        if (items == null || items.isEmpty())
            throw new BusinessRuleException("Purchase order requires at least one line.", "PROC_ORDER_LINE_REQUIRED", HttpStatus.CONFLICT);
        items.forEach(item -> {
            if (item.quantity() == null || item.quantity().signum() <= 0
                    || item.unitPrice() == null || item.unitPrice().signum() < 0)
                throw new BusinessRuleException("Purchase order quantities must be positive and prices cannot be negative.", "PROC_ORDER_LINE_VALUES_INVALID", HttpStatus.CONFLICT);
        });
    }

    private List<PurchaseOrderLine> buildLines(String purchaseOrderId,
                                               List<ProcurementApi.PurchaseOrderLinePayload> items) {
        return items.stream().map(item -> {
            var inventoryItem = operationsService.inventoryItem(item.itemId());
            if (!inventoryItem.active())
                throw new BusinessRuleException("Inactive inventory items cannot be purchased.", "PROC_INACTIVE_ITEM", HttpStatus.CONFLICT);
            String unit = inventoryItem.uomName() != null && !inventoryItem.uomName().isBlank()
                    ? inventoryItem.uomName() : inventoryItem.unitCode();
            return new PurchaseOrderLine(purchaseOrderId, inventoryItem.id(), inventoryItem.name(),
                    inventoryItem.categoryName(), item.quantity(), unit, item.unitPrice());
        }).toList();
    }

    private String nextDocumentNumber(String documentType) {
        var sequence = procurementDocumentSequenceRepository.findByDocumentType(documentType)
                .orElseGet(() -> procurementDocumentSequenceRepository.save(
                        new com.bemo.hr.trade.procurement.domain.ProcurementDocumentSequence(
                                documentType, highestExistingNumber(documentType) + 1)));
        return Long.toString(sequence.takeNext());
    }

    private long highestExistingNumber(String documentType) {
        var numbers = "PURCHASE_ORDER".equals(documentType)
                ? purchaseOrderRepository.findAll().stream().map(PurchaseOrder::getPoNumber)
                : goodsReceiptRepository.findAll().stream().map(GoodsReceipt::getGrnNumber);
        return numbers.mapToLong(this::trailingNumber).max().orElse(0);
    }

    private long trailingNumber(String value) {
        if (value == null) return 0;
        var matcher = TRAILING_NUMBER.matcher(value.strip());
        if (!matcher.find()) return 0;
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            // Fix V-07: Log and block unsafe initialization on overflow
            throw new com.bemo.hr.shared.domain.BusinessRuleException("Invalid document number format or overflow: " + value, "DOCUMENT_NUMBER_OVERFLOW", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }

    private String resolvePoNumber(String requested, String currentId) {
        if (automaticNumbering()) return currentId == null
                ? nextDocumentNumber("PURCHASE_ORDER") : requirePo(currentId).getPoNumber();
        String value = requireManualNumber(requested, "رقم أمر الشراء مطلوب عند اختيار الترقيم اليدوي.");
        boolean duplicate = currentId == null ? purchaseOrderRepository.existsByPoNumberIgnoreCase(value)
                : purchaseOrderRepository.existsByPoNumberIgnoreCaseAndIdNot(value, currentId);
        if (duplicate)
            throw new BusinessRuleException("رقم أمر الشراء مستخدم بالفعل.", "PROC_ORDER_NUMBER_EXISTS", HttpStatus.CONFLICT);
        return value;
    }

    private String resolveGrnNumber(String requested) {
        if (automaticNumbering()) return nextDocumentNumber("GOODS_RECEIPT");
        String value = requireManualNumber(requested, "رقم إذن الاستلام مطلوب عند اختيار الترقيم اليدوي.");
        if (goodsReceiptRepository.existsByGrnNumberIgnoreCase(value))
            throw new BusinessRuleException("رقم إذن الاستلام مستخدم بالفعل.", "PROC_GRN_NUMBER_EXISTS", HttpStatus.CONFLICT);
        return value;
    }

    private String requireManualNumber(String value, String message) {
        if (value == null || value.isBlank()) throw new BusinessRuleException(message);
        return value.strip().toUpperCase(java.util.Locale.ROOT);
    }

    private String resolvePaymentNumber(ProcurementApi.SupplierPaymentPayload payload, LocalDate paymentDate) {
        if (automaticDocumentNumbering()) {
            return documentNumberService.next("SUPPLIER_PAYMENT", "PMT", paymentDate);
        }
        String value = requireManualNumber(payload.paymentNumber(), "رقم الدفعة مطلوب عند اختيار الترقيم اليدوي.");
        if (supplierPaymentRepository.existsByPaymentNumberIgnoreCase(value))
            throw new BusinessRuleException("رقم الدفعة مستخدم بالفعل.", "PROC_PAYMENT_NUMBER_EXISTS", HttpStatus.CONFLICT);
        return value;
    }

    private boolean automaticNumbering() {
        return tenantApplicationRepository.findById(TenantContext.require())
                .orElseThrow(() -> new BusinessRuleException("Application settings were not found.", "PROC_APP_SETTINGS_NOT_FOUND", HttpStatus.CONFLICT))
                .isAutomaticProcurementNumbering();
    }

    private boolean automaticDocumentNumbering() {
        return tenantApplicationRepository.findById(TenantContext.require())
                .orElseThrow(() -> new BusinessRuleException("Application settings were not found.", "PROC_APP_SETTINGS_NOT_FOUND", HttpStatus.CONFLICT))
                .isAutomaticDocumentNumbering();
    }

    private String resolveSupplierName(String id) {
        if (id == null) return "—";
        var opt = businessPartyRepository.findById(id);
        return (opt != null && opt.isPresent() && opt.get().getName() != null) ? opt.get().getName() : id;
    }

    private Map<String, String> resolveNames(List<String> ids) {
        return ids.stream().distinct().collect(Collectors.toMap(id -> id, this::resolveSupplierName));
    }

    private Map<String, String> resolveSupplierNames(List<PurchaseOrder> orders) {
        return orders.stream().map(PurchaseOrder::getSupplierId).distinct()
                .collect(Collectors.toMap(id -> id, this::resolveSupplierName));
    }

    private List<PurchaseOrderLine> loadLines(String purchaseOrderId) {
        return purchaseOrderLineRepository.findByPurchaseOrderId(purchaseOrderId);
    }

    private long toEpochMs(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private ProcurementApi.PurchaseOrderResponse toPoResponse(PurchaseOrder po, List<PurchaseOrderLine> lines,
                                                              Map<String, String> supplierNames) {
        Map<String, BigDecimal> received = acceptedQuantities(po.getId());
        return new ProcurementApi.PurchaseOrderResponse(po.getId(), po.getPoNumber(), toEpochMs(po.getPoDate()),
                po.getSupplierId(), supplierNames.get(po.getSupplierId()), po.getPurchaseRequestId(),
                po.getDepartmentId(), po.getPaymentTerms(), po.getCurrencyCode(), po.getBaseCurrencyCode(), po.getExchangeRate(),
                toEpochMs(po.getExchangeRateDate()), po.getExchangeRateSource(), po.getExchangeRateOverrideReason(),
                po.getBaseTotalAmount(), po.getStatus().name(), po.getTotalAmount(),
                lines.stream().map(line -> toLineResponse(line, received)).toList(), po.getCreatedAt(), po.getUpdatedAt());
    }

    private ProcurementApi.PurchaseOrderLineResponse toLineResponse(PurchaseOrderLine line,
                                                                    Map<String, BigDecimal> received) {
        BigDecimal receivedQuantity = received.getOrDefault(line.getId(), BigDecimal.ZERO);
        BigDecimal remainingQuantity = line.getQuantity().subtract(receivedQuantity).max(BigDecimal.ZERO);
        return new ProcurementApi.PurchaseOrderLineResponse(line.getId(), line.getItemId(), line.getItemName(), line.getItemCategory(),
                line.getQuantity(), receivedQuantity, remainingQuantity, line.getUnitOfMeasure(),
                line.getUnitPrice(), line.getLineTotal());
    }

    private Map<String, BigDecimal> acceptedQuantities(String purchaseOrderId) {
        Map<String, BigDecimal> accepted = new HashMap<>();
        goodsReceiptRepository.findByPurchaseOrderId(purchaseOrderId).forEach(receipt ->
                receipt.getLines().forEach(line -> accepted.merge(
                        line.getPurchaseOrderLineId(), line.getQuantity(), BigDecimal::add)));
        return accepted;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private ProcurementApi.GoodsReceiptResponse toGrnResponse(GoodsReceipt grn, Map<String, String> supplierNames) {
        return new ProcurementApi.GoodsReceiptResponse(grn.getId(), grn.getGrnNumber(), toEpochMs(grn.getReceiptDate()),
                grn.getPurchaseOrderId(), grn.getSupplierId(), supplierNames.get(grn.getSupplierId()),
                grn.getWarehouseId(), grn.getStatus(), requirePo(grn.getPurchaseOrderId()).getCurrencyCode(), grn.getNotes(),
                grn.getLines().stream().map(l -> new ProcurementApi.GoodsReceiptLineResponse(
                        l.getId(), l.getPurchaseOrderLineId(), l.getItemId(), l.getItemName(), l.getItemCategory(),
                        l.getDeliveredQuantity(), l.getRejectedQuantity(), l.getDeductedQuantity(),
                        l.getQuantity(), l.getUnitOfMeasure(), l.getUnitPrice(),
                        l.getLocationId(), l.getLotNumber(), l.getQualityReason())).toList(),
                grn.getCreatedAt());
    }

    private ProcurementApi.SupplierInvoiceResponse toInvoiceResponse(SupplierInvoice inv, Map<String, String> supplierNames) {
        BigDecimal paidAmount = paidAmount(inv.getId());
        BigDecimal outstandingAmount = inv.getNetAmount().subtract(paidAmount).max(BigDecimal.ZERO);
        return new ProcurementApi.SupplierInvoiceResponse(inv.getId(), inv.getInvoiceNumber(), inv.getSupplierId(),
                supplierNames.get(inv.getSupplierId()), inv.getPurchaseOrderId(), inv.getGoodsReceiptId(),
                inv.getResponsiblePartyId(), inv.getInternalReference(), inv.getMissingInvoiceReason(), inv.getCurrencyCode(),
                inv.getBaseCurrencyCode(), inv.getExchangeRate(), toEpochMs(inv.getExchangeRateDate()),
                inv.getExchangeRateSource(), inv.getExchangeRateOverrideReason(), inv.getBaseNetAmount(),
                toEpochMs(inv.getInvoiceDate()),
                inv.getTotalAmount(), inv.getDiscountAmount(), inv.getTaxAmount(), inv.getNetAmount(),
                paidAmount, outstandingAmount,
                inv.getDueDate() != null ? toEpochMs(inv.getDueDate()) : null,
                inv.getNotes(), inv.getStatus(), inv.getCreatedAt(), inv.getUpdatedAt());
    }

    private ProcurementApi.SupplierPaymentResponse toPaymentResponse(SupplierPayment pmt, Map<String, String> supplierNames) {
        return new ProcurementApi.SupplierPaymentResponse(pmt.getId(), pmt.getPaymentNumber(),
                toEpochMs(pmt.getPaymentDate()), pmt.getSupplierId(), supplierNames.get(pmt.getSupplierId()),
                pmt.getSupplierInvoiceId(), pmt.getAmount(), supplierPaymentCurrency(pmt), pmt.getPaymentMethod(), pmt.getNotes(),
                pmt.getOperationId(), pmt.getStatus(), pmt.getCreatedAt());
    }

    private String supplierPaymentCurrency(SupplierPayment payment) {
        return supplierInvoiceRepository.findById(payment.getSupplierInvoiceId())
                .map(SupplierInvoice::getCurrencyCode).orElse("EGP");
    }

    private BigDecimal paidAmount(String invoiceId) {
        return supplierPaymentRepository.findBySupplierInvoiceId(invoiceId).stream()
                .filter(payment -> "POSTED".equals(payment.getStatus()))
                .map(SupplierPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName() : "system";
    }

    @Transactional
    public ProcurementApi.ThreeWayMatchResponse performThreeWayMatch(String supplierInvoiceId, BigDecimal tolerancePercentage) {
        log.debug("performThreeWayMatch called with supplierInvoiceId={}, tolerancePercentage={}", supplierInvoiceId, tolerancePercentage);
        SupplierInvoice invoice = supplierInvoiceRepository.findById(supplierInvoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + supplierInvoiceId, "SUPPLIER_INVOICE_NOT_FOUND"));

        PurchaseOrder po = purchaseOrderRepository.findById(invoice.getPurchaseOrderId())
                .orElseThrow(() -> new NotFoundException("PO not found for invoice", "PO_NOT_FOUND"));

        BigDecimal poAmount = po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal invoiceAmount = invoice.getNetAmount() != null ? invoice.getNetAmount() : BigDecimal.ZERO;
        BigDecimal priceVariance = invoiceAmount.subtract(poAmount).abs();

        BigDecimal tolerance = tolerancePercentage != null ? tolerancePercentage : BigDecimal.ZERO;
        BigDecimal maxAllowedVariance = poAmount.multiply(tolerance).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

        String matchStatus;
        String reason;
        if (priceVariance.compareTo(maxAllowedVariance) <= 0) {
            matchStatus = "MATCHED";
            reason = "Invoice 3-way match passed within " + tolerance + "% tolerance.";
        } else {
            matchStatus = "VARIANCE_EXCEEDED";
            reason = "Price variance (" + priceVariance + ") exceeds allowed tolerance threshold (" + maxAllowedVariance + ").";
        }

        var existingMatch = threeWayMatchRepository.findBySupplierInvoiceId(supplierInvoiceId);
        com.bemo.hr.trade.procurement.domain.ProcurementThreeWayMatch match;
        if (existingMatch.isPresent()) {
            match = existingMatch.get();
        } else {
            match = new com.bemo.hr.trade.procurement.domain.ProcurementThreeWayMatch(po.getId(), null, invoice.getId(), matchStatus, priceVariance, BigDecimal.ZERO, tolerance, reason);
        }
        match = threeWayMatchRepository.save(match);
        auditService.record("THREE_WAY_MATCH", "SUPPLIER_INVOICE", invoice.getId(), getCurrentUser(), "{\"status\":\"" + matchStatus + "\"}", null);
        log.info("ThreeWayMatch {} created with status {} for invoice {} successfully", match.getId(), matchStatus, supplierInvoiceId);

        return toThreeWayMatchResponse(match);
    }

    public ProcurementApi.ThreeWayMatchResponse getThreeWayMatch(String supplierInvoiceId) {
        log.debug("getThreeWayMatch called with supplierInvoiceId={}", supplierInvoiceId);
        var match = threeWayMatchRepository.findBySupplierInvoiceId(supplierInvoiceId)
                .orElseThrow(() -> {
                    log.warn("No 3-way match record found for invoiceId={}", supplierInvoiceId);
                    return new NotFoundException("No 3-way match record found for invoice", "THREE_WAY_MATCH_NOT_FOUND");
                });
        return toThreeWayMatchResponse(match);
    }

    @Transactional
    public ProcurementApi.ThreeWayMatchResponse resolveMatchVariance(String matchId, String resolutionNotes) {
        log.debug("resolveMatchVariance called with matchId={}", matchId);
        var match = threeWayMatchRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match record not found: " + matchId, "THREE_WAY_MATCH_NOT_FOUND"));
        match.resolve(getCurrentUser(), resolutionNotes);
        match = threeWayMatchRepository.save(match);
        auditService.record("RESOLVE_THREE_WAY_MATCH", "PROCUREMENT_MATCH", matchId, getCurrentUser(), "{\"notes\":\"" + resolutionNotes + "\"}", null);
        log.info("ThreeWayMatch {} resolved successfully", matchId);
        return toThreeWayMatchResponse(match);
    }

    private ProcurementApi.ThreeWayMatchResponse toThreeWayMatchResponse(com.bemo.hr.trade.procurement.domain.ProcurementThreeWayMatch match) {
        return new ProcurementApi.ThreeWayMatchResponse(
                match.getId(), match.getPurchaseOrderId(), match.getGoodsReceiptId(),
                match.getSupplierInvoiceId(), match.getMatchStatus(), match.getPriceVarianceAmount(),
                match.getQuantityVarianceAmount(), match.getTolerancePercentage(), match.getVarianceReason(),
                match.getResolvedBy(), match.getResolvedAt() != null ? match.getResolvedAt().toEpochMilli() : null,
                match.getCreatedAt().toEpochMilli()
        );
    }

    public List<ProcurementApi.SupplierReturnResponse> listSupplierReturns() {
        log.debug("listSupplierReturns called");
        List<SupplierReturn> returns = supplierReturnRepository.findAllByOrderByReturnDateDesc();
        Map<String, String> supplierNames = resolveNames(returns.stream().map(SupplierReturn::getSupplierId).distinct().toList());
        return returns.stream().map(ret -> toSupplierReturnResponse(ret, supplierNames)).toList();
    }

    // ─── Supplier Returns ──────────────────────────────────────────────

    @Transactional
    public ProcurementApi.SupplierReturnResponse createSupplierReturn(ProcurementApi.SupplierReturnPayload payload) {
        log.debug("createSupplierReturn called with purchaseOrderId={}, supplierId={}", payload.purchaseOrderId(), payload.supplierId());
        PurchaseOrder po = requirePo(payload.purchaseOrderId());
        if (!po.getSupplierId().equals(payload.supplierId()))
            throw new BusinessRuleException("Mismatched supplier for purchase order return", "PROC_RETURN_SUPPLIER_MISMATCH", HttpStatus.CONFLICT);
        if (payload.lines() == null || payload.lines().isEmpty())
            throw new BusinessRuleException("Supplier return requires at least one line.", "PROC_RETURN_LINE_REQUIRED", HttpStatus.CONFLICT);

        List<GoodsReceipt> grns = goodsReceiptRepository.findByPurchaseOrderId(po.getId());
        Map<String, BigDecimal> totalAcceptedPerPoLine = new HashMap<>();
        grns.forEach(grn -> grn.getLines().forEach(line ->
                totalAcceptedPerPoLine.merge(line.getPurchaseOrderLineId(), line.getQuantity(), BigDecimal::add)));

        List<SupplierReturn> existingReturns = supplierReturnRepository.findByPurchaseOrderId(po.getId());
        Map<String, BigDecimal> previouslyReturnedPerPoLine = new HashMap<>();
        existingReturns.forEach(ret -> ret.getLines().forEach(line ->
                previouslyReturnedPerPoLine.merge(line.getPurchaseOrderLineId(), line.getQuantity(), BigDecimal::add)));

        Map<String, PurchaseOrderLine> poLines = loadLines(po.getId()).stream()
                .collect(Collectors.toMap(PurchaseOrderLine::getId, line -> line));

        LocalDate returnDate = Instant.ofEpochMilli(payload.returnDate()).atZone(ZoneOffset.UTC).toLocalDate();

        List<SupplierReturnLine> lines = payload.lines().stream().map(line -> {
            PurchaseOrderLine poLine = poLines.get(line.purchaseOrderLineId());
            if (poLine == null)
                throw new BusinessRuleException("Supplier return line does not belong to the selected purchase order.", "PROC_RETURN_LINE_WRONG_ORDER", HttpStatus.CONFLICT);
            if (poLine.getItemId() == null || !poLine.getItemId().equals(line.itemId()))
                throw new BusinessRuleException("Supplier return inventory item must match its purchase-order line.", "PROC_RETURN_ITEM_MISMATCH", HttpStatus.CONFLICT);
            if (line.quantity() == null || line.quantity().signum() <= 0)
                throw new BusinessRuleException("كمية الإرجاع يجب أن تكون أكبر من صفر.", "PROC_RETURN_QUANTITY_POSITIVE", HttpStatus.CONFLICT);

            BigDecimal accepted = totalAcceptedPerPoLine.getOrDefault(poLine.getId(), BigDecimal.ZERO);
            BigDecimal returned = previouslyReturnedPerPoLine.getOrDefault(poLine.getId(), BigDecimal.ZERO);
            BigDecimal returnable = accepted.subtract(returned);
            if (returnable.signum() <= 0)
                throw new BusinessRuleException("لا توجد كمية قابلة للإرجاع للصنف: " + poLine.getItemName(), "PROC_RETURN_NO_RETURNABLE_QTY", HttpStatus.CONFLICT);
            if (line.quantity().compareTo(returnable) > 0)
                throw new BusinessRuleException("كمية الإرجاع تتجاوز المتاح للإرجاع للصنف: " + poLine.getItemName(), "PROC_RETURN_EXCEEDS_RETURNABLE", HttpStatus.CONFLICT);

            previouslyReturnedPerPoLine.merge(poLine.getId(), line.quantity(), BigDecimal::add);
            return new SupplierReturnLine(line.purchaseOrderLineId(), line.itemId(), poLine.getItemName(),
                    poLine.getItemCategory(), line.quantity(), poLine.getUnitOfMeasure(),
                    poLine.getUnitPrice(), normalizeOptional(line.locationId()), normalizeOptional(line.reason()));
        }).toList();

        SupplierReturn ret = new SupplierReturn(resolveReturnNumber(payload.returnNumber(), returnDate), returnDate,
                payload.purchaseOrderId(), payload.supplierId(), payload.warehouseId(), payload.notes(), lines);
        SupplierReturn saved = supplierReturnRepository.save(ret);

        String actor = getCurrentUser();
        lines.forEach(line ->
                operationsService.recordSupplierReturn(line.getItemId(), po.getSupplierId(), line.getQuantity(),
                        line.getUnitPrice(), saved.getReturnNumber(), line.getReason(),
                        returnDate.atStartOfDay(ZoneOffset.UTC).toInstant(), actor));

        BigDecimal totalNetAccepted = poLines.values().stream().map(poLine -> {
            BigDecimal acc = totalAcceptedPerPoLine.getOrDefault(poLine.getId(), BigDecimal.ZERO);
            BigDecimal retQty = previouslyReturnedPerPoLine.getOrDefault(poLine.getId(), BigDecimal.ZERO);
            return acc.subtract(retQty).max(BigDecimal.ZERO);
        }).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOrdered = poLines.values().stream().map(PurchaseOrderLine::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalNetAccepted.compareTo(totalOrdered) >= 0) {
            po.updateStatus(PurchaseOrder.Status.RECEIVED);
        } else if (totalNetAccepted.signum() > 0) {
            po.updateStatus(PurchaseOrder.Status.PARTIALLY_RECEIVED);
        } else {
            po.updateStatus(PurchaseOrder.Status.ISSUED);
        }

        auditService.record("CREATE", "SUPPLIER_RETURN", saved.getId(), getCurrentUser(),
                "{\"returnNumber\":\"" + saved.getReturnNumber() + "\",\"po\":\"" + po.getPoNumber() + "\"}", null);
        log.info("SupplierReturn {} created with number {} for PO {} successfully", saved.getId(), saved.getReturnNumber(), po.getPoNumber());
        return toSupplierReturnResponse(saved, resolveSupplierNames(List.of(po)));
    }

    private String resolveReturnNumber(String requested, LocalDate returnDate) {
        if (automaticDocumentNumbering()) {
            return documentNumberService.next("SUPPLIER_RETURN", "RET", returnDate);
        }
        String value = requireManualNumber(requested, "رقم إذن الإرجاع مطلوب عند اختيار الترقيم اليدوي.");
        if (supplierReturnRepository.existsByReturnNumberIgnoreCase(value))
            throw new BusinessRuleException("رقم إذن الإرجاع مستخدم بالفعل.", "PROC_RETURN_NUMBER_EXISTS", HttpStatus.CONFLICT);
        return value;
    }

    private ProcurementApi.SupplierReturnResponse toSupplierReturnResponse(SupplierReturn ret, Map<String, String> supplierNames) {
        List<ProcurementApi.SupplierReturnLineResponse> lineResponses = ret.getLines().stream().map(l ->
                new ProcurementApi.SupplierReturnLineResponse(l.getId(), l.getPurchaseOrderLineId(), l.getItemId(),
                        l.getItemName(), l.getItemCategory(), l.getQuantity(), l.getUnitOfMeasure(),
                        l.getUnitPrice(), l.getLocationId(), l.getReason())).toList();
        return new ProcurementApi.SupplierReturnResponse(ret.getId(), ret.getReturnNumber(),
                ret.getReturnDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                ret.getPurchaseOrderId(), ret.getSupplierId(), supplierNames.getOrDefault(ret.getSupplierId(), "—"),
                ret.getWarehouseId(), ret.getStatus(), ret.getNotes(), lineResponses, ret.getCreatedAt());
    }

    private record ExchangeRateSnapshot(String baseCurrencyCode, BigDecimal rate, LocalDate rateDate,
                                        String source, String overrideReason) {
    }
}
