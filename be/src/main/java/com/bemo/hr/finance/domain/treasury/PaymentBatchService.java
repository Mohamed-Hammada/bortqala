package com.bemo.hr.finance.domain.treasury;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentBatchService {

    private final PaymentBatchHeaderRepository batchHeaderRepository;
    private final PaymentBatchItemRepository batchItemRepository;

    public PaymentBatchService(PaymentBatchHeaderRepository batchHeaderRepository,
                               PaymentBatchItemRepository batchItemRepository) {
        this.batchHeaderRepository = batchHeaderRepository;
        this.batchItemRepository = batchItemRepository;
    }

    @Transactional
    public PaymentBatchHeader createBatch(String batchNumber, PaymentBatchHeader.SourceCategory sourceCategory, BigDecimal totalAmount) {
        PaymentBatchHeader batch = new PaymentBatchHeader(batchNumber, sourceCategory, totalAmount);
        return batchHeaderRepository.save(batch);
    }

    @Transactional
    public PaymentBatchItem addBatchItem(String batchId, String documentId, String payeeId, String payeeName, BigDecimal amount, String bankAccount) {
        PaymentBatchHeader batch = getBatch(batchId);
        if (batch.getStatus() != PaymentBatchHeader.Status.DRAFT) {
            throw new BusinessRuleException("Cannot add items to a non-DRAFT payment batch", "BATCH_NOT_DRAFT", HttpStatus.CONFLICT);
        }
        PaymentBatchItem item = new PaymentBatchItem(batchId, documentId, payeeId, payeeName, amount, bankAccount);
        return batchItemRepository.save(item);
    }

    @Transactional
    public PaymentBatchHeader submitBatch(String batchId) {
        PaymentBatchHeader batch = getBatch(batchId);
        batch.submit();
        return batchHeaderRepository.save(batch);
    }

    @Transactional
    public PaymentBatchHeader approveBatch(String batchId) {
        PaymentBatchHeader batch = getBatch(batchId);
        batch.approve();
        return batchHeaderRepository.save(batch);
    }

    @Transactional
    public PaymentBatchHeader rejectBatch(String batchId) {
        PaymentBatchHeader batch = getBatch(batchId);
        batch.reject();
        return batchHeaderRepository.save(batch);
    }

    @Transactional
    public PaymentBatchHeader disburseBatch(String batchId) {
        PaymentBatchHeader batch = getBatch(batchId);
        batch.disburse();
        return batchHeaderRepository.save(batch);
    }

    @Transactional(readOnly = true)
    public List<PaymentBatchItem> getBatchItems(String batchId) {
        return batchItemRepository.findByBatchId(batchId);
    }

    private PaymentBatchHeader getBatch(String id) {
        return batchHeaderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Payment batch not found", "BATCH_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
