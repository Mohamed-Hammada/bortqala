package com.bemo.hr.trade.procurement;

import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierInvoiceTests {
    @Test
    void tracksPartialAndFullPaymentStatus() {
        var invoice = new SupplierInvoice("INV-1", "supplier-1", null, null, null, LocalDate.of(2026, 7, 29),
                new BigDecimal("1000"), new BigDecimal("100"), BigDecimal.ZERO, null, null);

        invoice.updatePaymentStatus(new BigDecimal("400"));
        assertThat(invoice.getStatus()).isEqualTo("PARTIALLY_PAID");

        invoice.updatePaymentStatus(new BigDecimal("900"));
        assertThat(invoice.getStatus()).isEqualTo("PAID");
    }

    @Test
    void keepsSupplierInvoiceNumberEmptyForTransactionsWithoutAnInvoice() {
        var invoice = new SupplierInvoice(null, "INT-42", "المورد لم يصدر فاتورة", "EGP",
                "supplier-1", null, null, null, LocalDate.of(2026, 7, 29),
                new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO, null, null);

        assertThat(invoice.getInvoiceNumber()).isNull();
        assertThat(invoice.getInternalReference()).isEqualTo("INT-42");
        assertThat(invoice.getDocumentReference()).isEqualTo("INT-42");
    }
}
