package com.bemo.hr.finance.domain.treasury;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import com.bemo.hr.approval.SegregationOfDutiesService;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentRepository;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.payroll.infrastructure.PayrollPaymentBatchRepository;
import com.bemo.hr.workforce.ContractorSettlementRepository;

@Service
public class PaymentBatchService {

    private final PaymentBatchHeaderRepository batchHeaderRepository;
    private final PaymentBatchItemRepository batchItemRepository;
    private final PaymentBatchDisbursementRepository disbursementRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final PayrollPaymentBatchRepository payrollPaymentBatchRepository;
    private final ContractorSettlementRepository contractorSettlementRepository;
    private final SegregationOfDutiesService segregationOfDutiesService;
    private final AuditService auditService;

    public PaymentBatchService(PaymentBatchHeaderRepository batchHeaderRepository,
                               PaymentBatchItemRepository batchItemRepository,
                               PaymentBatchDisbursementRepository disbursementRepository,
                               SupplierInvoiceRepository supplierInvoiceRepository,
                               SupplierPaymentRepository supplierPaymentRepository,
                               PayrollPaymentBatchRepository payrollPaymentBatchRepository,
                               ContractorSettlementRepository contractorSettlementRepository,
                               SegregationOfDutiesService segregationOfDutiesService,
                               AuditService auditService) {
        this.batchHeaderRepository = batchHeaderRepository;
        this.batchItemRepository = batchItemRepository;
        this.disbursementRepository=disbursementRepository;this.supplierInvoiceRepository=supplierInvoiceRepository;
        this.supplierPaymentRepository=supplierPaymentRepository;this.payrollPaymentBatchRepository=payrollPaymentBatchRepository;
        this.contractorSettlementRepository=contractorSettlementRepository;this.segregationOfDutiesService=segregationOfDutiesService;
        this.auditService=auditService;
    }

    @Transactional
    public PaymentBatchHeader createBatch(String batchNumber, PaymentBatchHeader.SourceCategory sourceCategory, String actor) {
        PaymentBatchHeader batch = new PaymentBatchHeader(batchNumber, sourceCategory, actor);
        return batchHeaderRepository.save(batch);
    }

    @Transactional
    public PaymentBatchItem addBatchItem(String batchId, String documentId, String payeeId, String payeeName, BigDecimal amount, String bankAccount) {
        PaymentBatchHeader batch = getBatch(batchId);
        if (batch.getStatus() != PaymentBatchHeader.Status.DRAFT) {
            throw new BusinessRuleException("Cannot add items to a non-DRAFT payment batch", "BATCH_NOT_DRAFT", HttpStatus.CONFLICT);
        }
        if (batchItemRepository.existsByBatchIdAndDocumentId(batchId, documentId)) throw new BusinessRuleException(
                "Source document already exists in this batch", "BATCH_SOURCE_DUPLICATE", HttpStatus.CONFLICT);
        validateEligible(batch.getSourceCategory(), documentId, payeeId, amount);
        PaymentBatchItem item = new PaymentBatchItem(batchId, documentId, payeeId, payeeName, amount, bankAccount);
        return batchItemRepository.save(item);
    }

    @Transactional
    public PaymentBatchHeader submitBatch(String batchId) {
        PaymentBatchHeader batch = getBatch(batchId);
        List<PaymentBatchItem> items=batchItemRepository.findByBatchId(batchId);
        if(items.isEmpty()) throw new BusinessRuleException("Payment batch requires at least one source", "BATCH_ITEMS_REQUIRED", HttpStatus.CONFLICT);
        batch.deriveTotal(items.stream().map(PaymentBatchItem::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add));
        batch.submit();
        return batchHeaderRepository.save(batch);
    }

    @Transactional
    public PaymentBatchHeader approveBatch(String batchId, String actor) {
        PaymentBatchHeader batch = getBatch(batchId);
        segregationOfDutiesService.validateRequesterNotApprover(batch.getCreatedBy(),actor,false);
        batch.approve(actor);
        return batchHeaderRepository.save(batch);
    }

    @Transactional
    public PaymentBatchHeader rejectBatch(String batchId) {
        PaymentBatchHeader batch = getBatch(batchId);
        batch.reject();
        return batchHeaderRepository.save(batch);
    }

    @Transactional
    public PaymentBatchHeader disburseBatch(String batchId, String operationId, String actor) {
        PaymentBatchHeader replay=batchHeaderRepository.findByOperationId(operationId).orElse(null);
        if(replay!=null)return replay;
        PaymentBatchHeader batch = getBatch(batchId);
        segregationOfDutiesService.validateCreatorNotPoster(batch.getCreatedBy(),actor,"payment-batch disbursement");
        segregationOfDutiesService.validateCreatorNotPoster(batch.getApprovedBy(),actor,"payment-batch disbursement");
        List<PaymentBatchItem> items=batchItemRepository.findByBatchId(batchId);
        for(int i=0;i<items.size();i++){
            PaymentBatchItem item=items.get(i);
            PaymentBatchDisbursement record=disbursementRepository.save(new PaymentBatchDisbursement(batchId,item.getId(),item.getDocumentId(),
                    item.getPayeeId(),item.getAmount(),item.getBankAccount(),operationId+":"+(i+1)));
            item.linkDisbursement(record.getId());batchItemRepository.save(item);
        }
        batch.disburse(operationId,actor);
        auditService.record("PAYMENT_BATCH_DISBURSED","PAYMENT_BATCH",batch.getId(),actor,
                "{\"approvedBy\":\""+batch.getApprovedBy()+"\",\"operationId\":\""+operationId+"\"}",null);
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

    private void validateEligible(PaymentBatchHeader.SourceCategory category,String documentId,String payeeId,BigDecimal amount){
        if(amount==null||amount.signum()<=0)throw new BusinessRuleException("Payment source amount must be positive","BATCH_SOURCE_INELIGIBLE",HttpStatus.CONFLICT);
        switch(category){
            case ACCOUNTS_PAYABLE -> { SupplierInvoice inv=supplierInvoiceRepository.findById(documentId).orElseThrow(()->new BusinessRuleException("Approved payable source not found","BATCH_SOURCE_INELIGIBLE",HttpStatus.CONFLICT));
                if(!inv.getSupplierId().equals(payeeId)||inv.getStatus().equals(SupplierInvoice.Status.PAID.name())||inv.getStatus().equals(SupplierInvoice.Status.CANCELLED.name()))throw new BusinessRuleException("Invoice is not eligible","BATCH_SOURCE_INELIGIBLE",HttpStatus.CONFLICT);
                BigDecimal paid=supplierPaymentRepository.sumPostedAmountBySupplierInvoiceId(documentId);if(amount.compareTo(inv.getNetAmount().subtract(paid==null?BigDecimal.ZERO:paid))>0)throw new BusinessRuleException("Amount exceeds payable balance","BATCH_SOURCE_INELIGIBLE",HttpStatus.CONFLICT); }
            case PAYROLL -> { var p=payrollPaymentBatchRepository.findById(documentId).orElseThrow(()->new BusinessRuleException("Processed payroll source not found","BATCH_SOURCE_INELIGIBLE",HttpStatus.CONFLICT));if(p.getStatus()!=com.bemo.hr.payroll.domain.PayrollPaymentBatch.Status.PROCESSED)throw new BusinessRuleException("Payroll source is not processed","BATCH_SOURCE_INELIGIBLE",HttpStatus.CONFLICT); }
            case WORKFORCE_CONTRACTOR -> { var s=contractorSettlementRepository.findById(documentId).orElseThrow(()->new BusinessRuleException("Posted contractor source not found","BATCH_SOURCE_INELIGIBLE",HttpStatus.CONFLICT));if(!"POSTED".equals(s.getStatus()))throw new BusinessRuleException("Contractor source is not posted","BATCH_SOURCE_INELIGIBLE",HttpStatus.CONFLICT); }
        }
    }
}
