package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.domain.SupplierPaymentPlan;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentPlanRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class SupplierPaymentPlanService {

    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierPaymentPlanRepository supplierPaymentPlanRepository;
    private final AuditService auditService;

    public SupplierPaymentPlanService(SupplierInvoiceRepository supplierInvoiceRepository,
                                      SupplierPaymentRepository supplierPaymentRepository,
                                      SupplierPaymentPlanRepository supplierPaymentPlanRepository,
                                      AuditService auditService) {
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.supplierPaymentPlanRepository = supplierPaymentPlanRepository;
        this.auditService = auditService;
    }

    public List<ProcurementApi.SupplierPaymentPlanResponse> listPaymentPlans(String invoiceId) {
        return supplierPaymentPlanRepository.findByInvoiceIdOrderByInstallmentNoAsc(invoiceId).stream()
                .map(SupplierPaymentPlanService::toResponse)
                .toList();
    }

    @Transactional
    public List<ProcurementApi.SupplierPaymentPlanResponse> createPaymentPlan(
            String invoiceId, ProcurementApi.SupplierPaymentPlanPayload payload) {
        log.debug("createPaymentPlan called for invoice {} with {} installments", invoiceId, payload.installmentCount());
        if (supplierPaymentPlanRepository.existsByInvoiceId(invoiceId))
            throw new BusinessRuleException("An installment plan already exists for this invoice.",
                    "PROC_PAYMENT_PLAN_ALREADY_EXISTS", HttpStatus.CONFLICT);
        SupplierInvoice invoice = supplierInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Invoice not found.", "PROC_INVOICE_NOT_FOUND", HttpStatus.CONFLICT));
        if ("CANCELLED".equals(invoice.getStatus()))
            throw new BusinessRuleException("Invoice is cancelled; no installment plan can be created.",
                    "PROC_INVOICE_ALREADY_PAID", HttpStatus.CONFLICT);

        BigDecimal remaining = outstandingAmount(invoice);
        if (remaining.signum() <= 0)
            throw new BusinessRuleException("Invoice is already paid or cancelled.", "PROC_INVOICE_ALREADY_PAID", HttpStatus.CONFLICT);
        int count = payload.installmentCount();
        LocalDate firstDueDate = Instant.ofEpochMilli(payload.firstDueDate()).atZone(ZoneOffset.UTC).toLocalDate();

        List<SupplierPaymentPlan> installments = new ArrayList<>(count);
        BigDecimal base = remaining.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 1; i <= count; i++) {
            BigDecimal amount = i == count ? remaining.subtract(allocated) : base;
            allocated = allocated.add(amount);
            installments.add(new SupplierPaymentPlan(invoiceId, i, firstDueDate.plusMonths(i - 1L), amount));
        }
        List<SupplierPaymentPlan> saved = supplierPaymentPlanRepository.saveAll(installments);

        auditService.record("CREATE", "SUPPLIER_PAYMENT_PLAN", invoiceId, getCurrentUser(),
                "{\"installmentCount\":" + count + ",\"remaining\":" + remaining
                        + ",\"firstDueDate\":\"" + firstDueDate + "\"}", null);
        log.info("Installment plan with {} installments created for invoice {}", saved.size(), invoiceId);
        return saved.stream().map(SupplierPaymentPlanService::toResponse).toList();
    }

    /**
     * Marks scheduled installments as paid once cumulative payments on the invoice
     * reach their amounts (WP-01: paying one installment via the normal payment flow).
     */
    @Transactional
    public void markInstallmentsSettled(String invoiceId, BigDecimal cumulativePaid) {
        if (cumulativePaid == null || cumulativePaid.signum() <= 0) return;
        List<SupplierPaymentPlan> plans = supplierPaymentPlanRepository.findByInvoiceIdOrderByInstallmentNoAsc(invoiceId);
        long now = System.currentTimeMillis();
        BigDecimal covered = BigDecimal.ZERO;
        boolean changed = false;
        for (SupplierPaymentPlan plan : plans) {
            covered = covered.add(plan.getAmount());
            if (cumulativePaid.compareTo(covered) >= 0 && !plan.isPaid()) {
                plan.markPaid(now);
                changed = true;
            }
        }
        if (changed) auditService.record("UPDATE", "SUPPLIER_PAYMENT_PLAN", invoiceId, getCurrentUser(),
                "{\"settledAgainst\":" + cumulativePaid + "}", null);
    }

    private static ProcurementApi.SupplierPaymentPlanResponse toResponse(SupplierPaymentPlan plan) {
        return new ProcurementApi.SupplierPaymentPlanResponse(plan.getId(), plan.getInvoiceId(),
                plan.getInstallmentNo(),
                plan.getDueDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                plan.getAmount(), plan.getPaidAt());
    }

    private BigDecimal outstandingAmount(SupplierInvoice invoice) {
        BigDecimal net = invoice.getNetAmount() == null ? BigDecimal.ZERO : invoice.getNetAmount();
        BigDecimal paid = supplierPaymentRepository.findBySupplierInvoiceId(invoice.getId()).stream()
                .filter(payment -> "POSTED".equals(payment.getStatus()))
                .map(payment -> payment.getAmount().add(payment.getSettlementDiscount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return net.subtract(paid);
    }

    private String getCurrentUser() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (authentication != null && authentication.getName() != null && !authentication.getName().isBlank())
                ? authentication.getName() : "system";
    }
}
