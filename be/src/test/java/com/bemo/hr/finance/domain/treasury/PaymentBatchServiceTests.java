package com.bemo.hr.finance.domain.treasury;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentBatchServiceTests {

    private PaymentBatchHeaderRepository batchHeaderRepository;
    private PaymentBatchItemRepository batchItemRepository;
    private PaymentBatchService paymentBatchService;
    private PaymentBatchDisbursementRepository disbursementRepository;
    private com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository supplierInvoiceRepository;
    private com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentRepository supplierPaymentRepository;
    private com.bemo.hr.payroll.infrastructure.PayrollPaymentBatchRepository payrollPaymentBatchRepository;
    private com.bemo.hr.workforce.ContractorSettlementRepository contractorSettlementRepository;

    @BeforeEach
    void setUp() {
        batchHeaderRepository = mock(PaymentBatchHeaderRepository.class);
        batchItemRepository = mock(PaymentBatchItemRepository.class);
        disbursementRepository = mock(PaymentBatchDisbursementRepository.class);
        supplierInvoiceRepository = mock(com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository.class);
        supplierPaymentRepository = mock(com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentRepository.class);
        payrollPaymentBatchRepository = mock(com.bemo.hr.payroll.infrastructure.PayrollPaymentBatchRepository.class);
        contractorSettlementRepository = mock(com.bemo.hr.workforce.ContractorSettlementRepository.class);
        paymentBatchService = new PaymentBatchService(batchHeaderRepository, batchItemRepository, disbursementRepository,
                supplierInvoiceRepository, supplierPaymentRepository, payrollPaymentBatchRepository, contractorSettlementRepository,
                new com.bemo.hr.approval.SegregationOfDutiesService(), mock(com.bemo.hr.audit.application.AuditService.class));
    }

    @Test
    void createsSubmitsApprovesAndDisbursesPaymentBatchSuccessfully() {
        PaymentBatchHeader header = new PaymentBatchHeader("BATCH-001", PaymentBatchHeader.SourceCategory.PAYROLL, "maker");
        when(batchHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(batchHeaderRepository.findById("batch-1")).thenReturn(Optional.of(header));
        when(batchItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(disbursementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        com.bemo.hr.payroll.domain.PayrollPaymentBatch payroll = new com.bemo.hr.payroll.domain.PayrollPaymentBatch("period-1", new BigDecimal("5000"), 5);
        payroll.process();
        when(payrollPaymentBatchRepository.findById("inv-10")).thenReturn(Optional.of(payroll));

        PaymentBatchHeader created = paymentBatchService.createBatch("BATCH-001", PaymentBatchHeader.SourceCategory.PAYROLL, "maker");
        assertThat(created.getStatus()).isEqualTo(PaymentBatchHeader.Status.DRAFT);

        PaymentBatchItem item = paymentBatchService.addBatchItem("batch-1", "inv-10", "supp-5", "Supplier Five", new BigDecimal("5000.00"), "EG1234567890");
        assertThat(item).isNotNull();
        assertThat(item.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        when(batchItemRepository.findByBatchId("batch-1")).thenReturn(java.util.List.of(item));

        paymentBatchService.submitBatch("batch-1");
        assertThat(header.getStatus()).isEqualTo(PaymentBatchHeader.Status.SUBMITTED);

        paymentBatchService.approveBatch("batch-1", "checker");
        assertThat(header.getStatus()).isEqualTo(PaymentBatchHeader.Status.APPROVED);

        paymentBatchService.disburseBatch("batch-1", "op-1", "disburser");
        assertThat(header.getStatus()).isEqualTo(PaymentBatchHeader.Status.DISBURSED);
        assertThat(header.getTotalAmount()).isEqualByComparingTo("5000");
        verify(disbursementRepository).save(any(PaymentBatchDisbursement.class));
        when(batchHeaderRepository.findByOperationId("op-1")).thenReturn(Optional.of(header));
        PaymentBatchHeader replay = paymentBatchService.disburseBatch("batch-1", "op-1", "disburser");
        assertThat(replay).isSameAs(header);
        verify(disbursementRepository, times(1)).save(any(PaymentBatchDisbursement.class));
    }

    @Test
    void rejectsDuplicateSourceAndMakerApproval() {
        PaymentBatchHeader header = new PaymentBatchHeader("BATCH-2", PaymentBatchHeader.SourceCategory.PAYROLL, "maker");
        when(batchHeaderRepository.findById("batch-2")).thenReturn(Optional.of(header));
        when(batchItemRepository.existsByBatchIdAndDocumentId("batch-2", "payroll-1")).thenReturn(true);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> paymentBatchService.addBatchItem("batch-2", "payroll-1",
                        "company", "Payroll", BigDecimal.ONE, "BANK"))
                .isInstanceOf(com.bemo.hr.shared.domain.BusinessRuleException.class);
        header.submit();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> paymentBatchService.approveBatch("batch-2", "maker"))
                .isInstanceOf(com.bemo.hr.approval.SegregationOfDutiesViolationException.class);
    }
}
