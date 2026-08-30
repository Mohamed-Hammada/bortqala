package com.bemo.hr.trade.procurement;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.application.SupplierPaymentPlanService;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.domain.SupplierPayment;
import com.bemo.hr.trade.procurement.domain.SupplierPaymentPlan;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentPlanRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierPaymentPlanTests {

    private SupplierInvoiceRepository supplierInvoiceRepository;
    private SupplierPaymentRepository supplierPaymentRepository;
    private SupplierPaymentPlanRepository supplierPaymentPlanRepository;
    private AuditService auditService;
    private SupplierPaymentPlanService planService;
    private SupplierInvoice invoice;

    @BeforeEach
    void setUp() {
        supplierInvoiceRepository = mock(SupplierInvoiceRepository.class);
        supplierPaymentRepository = mock(SupplierPaymentRepository.class);
        supplierPaymentPlanRepository = mock(SupplierPaymentPlanRepository.class);
        auditService = mock(AuditService.class);
        planService = new SupplierPaymentPlanService(supplierInvoiceRepository, supplierPaymentRepository,
                supplierPaymentPlanRepository, auditService);
        invoice = new SupplierInvoice("INV-100", "INV-100", null, "EGP", "supplier-a", null,
                null, null, LocalDate.of(2026, 8, 1), new BigDecimal("10000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        when(supplierInvoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(supplierPaymentRepository.findBySupplierInvoiceId(invoice.getId())).thenReturn(List.of());
        when(supplierPaymentPlanRepository.existsByInvoiceId(invoice.getId())).thenReturn(false);
        when(supplierPaymentPlanRepository.saveAll(any())).thenAnswer(invocation -> {
            List<SupplierPaymentPlan> plans = new ArrayList<>(invocation.getArgument(0));
            for (SupplierPaymentPlan plan : plans) plans.set(plans.indexOf(plan), plan);
            return invocation.getArgument(0);
        });
    }

    private long dueDate(int year, int month, int day) {
        return LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    @ParameterizedTest(name = "count={0} over {1} splits into {0} rows summing to remaining")
    @CsvSource({
            "3, 10000.00",
            "4, 9999.99",
            "2, 10000.00"
    })
    void createsEqualInstallmentsSummingToRemaining(int count, String net) {
        SupplierInvoice custom = new SupplierInvoice("INV-C", "INV-C", null, "EGP", "supplier-a", null,
                null, null, LocalDate.of(2026, 8, 1), new BigDecimal(net),
                BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        when(supplierInvoiceRepository.findById(custom.getId())).thenReturn(Optional.of(custom));

        List<ProcurementApi.SupplierPaymentPlanResponse> plan =
                planService.createPaymentPlan(custom.getId(), payload(count, dueDate(2026, 9, 1)));

        assertThat(plan).hasSize(count);
        assertThat(plan).extracting(ProcurementApi.SupplierPaymentPlanResponse::installmentNo)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, count).boxed().toList());
        BigDecimal total = plan.stream()
                .map(ProcurementApi.SupplierPaymentPlanResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo(new BigDecimal(net));
        assertThat(plan.get(plan.size() - 1).amount())
                .isGreaterThanOrEqualTo(plan.get(0).amount());
    }

    private ProcurementApi.SupplierPaymentPlanPayload payload(int count, long firstDueDate) {
        return new ProcurementApi.SupplierPaymentPlanPayload(count, firstDueDate);
    }

    @Test
    void monthlyDueDatesStartAtFirstDueDateAndIncrement() {
        List<ProcurementApi.SupplierPaymentPlanResponse> plan =
                planService.createPaymentPlan(invoice.getId(), payload(3, dueDate(2026, 9, 15)));
        assertThat(plan).extracting(ProcurementApi.SupplierPaymentPlanResponse::dueDate)
                .containsExactly(dueDate(2026, 9, 15), dueDate(2026, 10, 15), dueDate(2026, 11, 15));
    }

    @Test
    void partialPaymentsReducePlannedRemainder() {
        SupplierPayment posted = new SupplierPayment("PMT-1", LocalDate.of(2026, 8, 10),
                "supplier-a", invoice.getId(), "op-partial", new BigDecimal("4000.00"), "CASH", null);
        when(supplierPaymentRepository.findBySupplierInvoiceId(invoice.getId())).thenReturn(List.of(posted));

        List<ProcurementApi.SupplierPaymentPlanResponse> plan =
                planService.createPaymentPlan(invoice.getId(), payload(3, dueDate(2026, 9, 1)));
        BigDecimal total = plan.stream()
                .map(ProcurementApi.SupplierPaymentPlanResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo(new BigDecimal("6000.00"));
    }

    @Test
    void rejectsSecondPlanForSameInvoice() {
        when(supplierPaymentPlanRepository.existsByInvoiceId(invoice.getId())).thenReturn(true);
        assertThatThrownBy(() -> planService.createPaymentPlan(invoice.getId(), payload(3, dueDate(2026, 9, 1))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("PROC_PAYMENT_PLAN_ALREADY_EXISTS");
    }

    @Test
    void rejectsPlansOnFullyPaidInvoices() {
        SupplierPayment full = new SupplierPayment("PMT-F", LocalDate.of(2026, 8, 10),
                "supplier-a", invoice.getId(), "op-full", new BigDecimal("10000.00"), "CASH", null);
        invoice.updatePaymentStatus(full.getAmount());
        when(supplierPaymentRepository.findBySupplierInvoiceId(invoice.getId())).thenReturn(List.of(full));
        assertThatThrownBy(() -> planService.createPaymentPlan(invoice.getId(), payload(3, dueDate(2026, 9, 1))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("PROC_INVOICE_ALREADY_PAID");
    }

    @Test
    void missingInvoiceFallsBackToNotFoundCode() {
        when(supplierInvoiceRepository.findById("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> planService.createPaymentPlan("ghost", payload(3, dueDate(2026, 9, 1))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("PROC_INVOICE_NOT_FOUND");
    }

    @Test
    void cumulativePaymentsMarkInstallmentsPaidInOrder() {
        SupplierPaymentPlan first = new SupplierPaymentPlan(invoice.getId(), 1, LocalDate.of(2026, 9, 1), new BigDecimal("3333.33"));
        SupplierPaymentPlan second = new SupplierPaymentPlan(invoice.getId(), 2, LocalDate.of(2026, 10, 1), new BigDecimal("3333.33"));
        SupplierPaymentPlan third = new SupplierPaymentPlan(invoice.getId(), 3, LocalDate.of(2026, 11, 1), new BigDecimal("3333.34"));
        when(supplierPaymentPlanRepository.findByInvoiceIdOrderByInstallmentNoAsc(invoice.getId()))
                .thenReturn(List.of(first, second, third));

        planService.markInstallmentsSettled(invoice.getId(), new BigDecimal("4000.00"));
        assertThat(first.isPaid()).isTrue();
        assertThat(second.isPaid()).isFalse();
        assertThat(third.isPaid()).isFalse();

        planService.markInstallmentsSettled(invoice.getId(), new BigDecimal("10000.00"));
        assertThat(first.isPaid()).isTrue();
        assertThat(second.isPaid()).isTrue();
        assertThat(third.isPaid()).isTrue();

        verify(auditService, org.mockito.Mockito.times(2)).record(anyString(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void markSettledIgnoresZeroAndNegativeAmounts() {
        SupplierPaymentPlan only = new SupplierPaymentPlan(invoice.getId(), 1, LocalDate.of(2026, 9, 1), new BigDecimal("100.00"));
        when(supplierPaymentPlanRepository.findByInvoiceIdOrderByInstallmentNoAsc(invoice.getId()))
                .thenReturn(List.of(only));
        planService.markInstallmentsSettled(invoice.getId(), BigDecimal.ZERO);
        assertThat(only.isPaid()).isFalse();
        planService.markInstallmentsSettled(invoice.getId(), null);
        assertThat(only.isPaid()).isFalse();
    }

    @Test
    void listReturnsRowsOrderedByInstallmentNo() {
        when(supplierPaymentPlanRepository.findByInvoiceIdOrderByInstallmentNoAsc(invoice.getId()))
                .thenReturn(List.of(
                        new SupplierPaymentPlan(invoice.getId(), 1, LocalDate.of(2026, 9, 1), new BigDecimal("5000.00")),
                        new SupplierPaymentPlan(invoice.getId(), 2, LocalDate.of(2026, 10, 1), new BigDecimal("5000.00"))));
        List<ProcurementApi.SupplierPaymentPlanResponse> rows = planService.listPaymentPlans(invoice.getId());
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).paidAt()).isNull();
        assertThat(rows.get(0).dueDate()).isEqualTo(dueDate(2026, 9, 1));
    }
}
