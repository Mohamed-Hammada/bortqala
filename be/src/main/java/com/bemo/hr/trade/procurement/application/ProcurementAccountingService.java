package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.finance.domain.posting.SubledgerPostingService;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.domain.SupplierPayment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Slf4j
@Service
public class ProcurementAccountingService {

    private final SubledgerPostingService subledgerPostingService;

    public ProcurementAccountingService(SubledgerPostingService subledgerPostingService) {
        this.subledgerPostingService = subledgerPostingService;
    }

    @Transactional
    public void postSupplierInvoice(SupplierInvoice invoice, String actor) {
        log.debug("postSupplierInvoice called with invoiceId={}, actor={}", invoice.getId(), actor);
        subledgerPostingService.postSubledgerEvent(
                "PROCUREMENT",
                "SUPPLIER_INVOICE",
                invoice.getId(),
                "SUPPLIER_INVOICE_RECORDED",
                "AP:INVOICE:" + invoice.getId(),
                invoice.getInvoiceDate(),
                "Supplier invoice " + invoice.getDocumentReference(),
                invoice.getBaseNetAmount(),
                invoice.getBaseNetAmount(),
                null,
                invoice.getSupplierId(),
                invoice.getBaseCurrencyCode(),
                actor
        );
        log.info("Supplier invoice {} posted to subledger by {}", invoice.getId(), actor);
    }

    @Transactional
    public void postSupplierPayment(SupplierPayment payment, SupplierInvoice invoice, String actor) {
        log.debug("postSupplierPayment called with paymentId={}, actor={}", payment.getId(), actor);
        String method = normalizeMethod(payment.getPaymentMethod());
        BigDecimal baseAmount = payment.getAmount()
                .multiply(invoice.getExchangeRate())
                .setScale(2, RoundingMode.HALF_UP);
        subledgerPostingService.postSubledgerEvent(
                "PROCUREMENT",
                "SUPPLIER_PAYMENT",
                payment.getId(),
                "SUPPLIER_PAYMENT_" + method,
                "AP:PAYMENT:" + payment.getOperationId(),
                payment.getPaymentDate(),
                "Supplier payment " + payment.getPaymentNumber(),
                baseAmount,
                baseAmount,
                null,
                payment.getSupplierId(),
                invoice.getBaseCurrencyCode(),
                actor
        );
        log.info("Supplier payment {} posted to subledger by {}", payment.getId(), actor);
    }

    @Transactional
    public void postSupplierSettlementDiscount(SupplierPayment payment, SupplierInvoice invoice,
                                               BigDecimal discount, String actor) {
        BigDecimal discountBase = discount.multiply(invoice.getExchangeRate()).setScale(2, RoundingMode.HALF_UP);
        if (discountBase.signum() <= 0)
            return;
        log.debug("postSupplierSettlementDiscount called with paymentId={}, actor={}", payment.getId(), actor);
        subledgerPostingService.postSubledgerEvent(
                "PROCUREMENT",
                "SUPPLIER_SETTLEMENT_DISCOUNT",
                payment.getId(),
                "SUPPLIER_SETTLEMENT_DISCOUNT",
                "AP:DISCOUNT:" + payment.getOperationId(),
                payment.getPaymentDate(),
                "Settlement discount on invoice " + invoice.getInvoiceNumber()
                        + " via payment " + payment.getPaymentNumber(),
                discountBase,
                discountBase,
                null,
                payment.getSupplierId(),
                invoice.getBaseCurrencyCode(),
                actor
        );
        log.info("Supplier settlement discount {} posted to subledger by {}", payment.getId(), actor);
    }

    private String normalizeMethod(String value) {
        String normalized = value == null || value.isBlank()
                ? "CASH"
                : value.strip().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? "CASH" : normalized;
    }
}
