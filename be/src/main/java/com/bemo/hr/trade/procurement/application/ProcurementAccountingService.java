package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.finance.domain.posting.SubledgerPostingService;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.domain.SupplierPayment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
public class ProcurementAccountingService {

    private final SubledgerPostingService subledgerPostingService;

    public ProcurementAccountingService(SubledgerPostingService subledgerPostingService) {
        this.subledgerPostingService = subledgerPostingService;
    }

    @Transactional
    public void postSupplierInvoice(SupplierInvoice invoice, String actor) {
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
    }

    @Transactional
    public void postSupplierPayment(SupplierPayment payment, SupplierInvoice invoice, String actor) {
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
    }

    private String normalizeMethod(String value) {
        String normalized = value == null || value.isBlank()
                ? "CASH"
                : value.strip().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? "CASH" : normalized;
    }
}
