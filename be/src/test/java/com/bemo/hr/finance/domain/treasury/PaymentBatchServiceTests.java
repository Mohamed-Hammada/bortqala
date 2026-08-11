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

    @BeforeEach
    void setUp() {
        batchHeaderRepository = mock(PaymentBatchHeaderRepository.class);
        batchItemRepository = mock(PaymentBatchItemRepository.class);
        paymentBatchService = new PaymentBatchService(batchHeaderRepository, batchItemRepository);
    }

    @Test
    void createsSubmitsApprovesAndDisbursesPaymentBatchSuccessfully() {
        PaymentBatchHeader header = new PaymentBatchHeader("BATCH-001", PaymentBatchHeader.SourceCategory.ACCOUNTS_PAYABLE, new BigDecimal("5000.00"));
        when(batchHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(batchHeaderRepository.findById("batch-1")).thenReturn(Optional.of(header));
        when(batchItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentBatchHeader created = paymentBatchService.createBatch("BATCH-001", PaymentBatchHeader.SourceCategory.ACCOUNTS_PAYABLE, new BigDecimal("5000.00"));
        assertThat(created.getStatus()).isEqualTo(PaymentBatchHeader.Status.DRAFT);

        PaymentBatchItem item = paymentBatchService.addBatchItem("batch-1", "inv-10", "supp-5", "Supplier Five", new BigDecimal("5000.00"), "EG1234567890");
        assertThat(item).isNotNull();
        assertThat(item.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));

        paymentBatchService.submitBatch("batch-1");
        assertThat(header.getStatus()).isEqualTo(PaymentBatchHeader.Status.SUBMITTED);

        paymentBatchService.approveBatch("batch-1");
        assertThat(header.getStatus()).isEqualTo(PaymentBatchHeader.Status.APPROVED);

        paymentBatchService.disburseBatch("batch-1");
        assertThat(header.getStatus()).isEqualTo(PaymentBatchHeader.Status.DISBURSED);
    }
}
