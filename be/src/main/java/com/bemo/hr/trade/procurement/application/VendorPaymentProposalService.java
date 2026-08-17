package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.approval.SegregationOfDutiesService;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.domain.VendorPaymentProposal;
import com.bemo.hr.trade.procurement.domain.VendorPaymentProposalAllocation;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentRepository;
import com.bemo.hr.trade.procurement.infrastructure.VendorPaymentProposalAllocationRepository;
import com.bemo.hr.trade.procurement.infrastructure.VendorPaymentProposalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class VendorPaymentProposalService {

    private final VendorPaymentProposalRepository repository;
    private final VendorPaymentProposalAllocationRepository allocationRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final ProcurementService procurementService;
    private final SegregationOfDutiesService segregationOfDutiesService;
    private final AuditService auditService;

    public VendorPaymentProposalService(VendorPaymentProposalRepository repository,
                                        VendorPaymentProposalAllocationRepository allocationRepository,
                                        SupplierInvoiceRepository supplierInvoiceRepository,
                                        SupplierPaymentRepository supplierPaymentRepository,
                                        ProcurementService procurementService,
                                        SegregationOfDutiesService segregationOfDutiesService,
                                        AuditService auditService) {
        this.repository = repository;
        this.allocationRepository = allocationRepository;
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.procurementService = procurementService;
        this.segregationOfDutiesService = segregationOfDutiesService;
        this.auditService = auditService;
    }

    @Transactional
    public ProposalResult createProposal(String supplierId, List<AllocationInput> requestedAllocations,
                                         LocalDate dueDate, String actor) {
        ValidatedAllocations validated = validateAllocations(supplierId, requestedAllocations);
        List<AllocationInput> allocations = validated.allocations();
        BigDecimal total = allocations.stream().map(AllocationInput::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        VendorPaymentProposal proposal = repository.save(new VendorPaymentProposal(
                supplierId, allocations.get(0).invoiceId(), total, validated.currencyCode(), dueDate, actor));
        List<VendorPaymentProposalAllocation> savedAllocations = new java.util.ArrayList<>();
        for (int index = 0; index < allocations.size(); index++) {
            AllocationInput allocation = allocations.get(index);
            savedAllocations.add(allocationRepository.save(new VendorPaymentProposalAllocation(
                    proposal.getId(), index + 1, allocation.invoiceId(), allocation.amount())));
        }
        auditService.record("CREATE", "VENDOR_PAYMENT_PROPOSAL", proposal.getId(), actor,
                "{\"allocationCount\":" + savedAllocations.size() + ",\"amount\":" + total + "}", null);
        return result(proposal, savedAllocations);
    }

    @Transactional
    public ProposalResult approveProposal(String id, String actor) {
        VendorPaymentProposal proposal = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessRuleException("Payment proposal not found", "PAYMENT_PROPOSAL_NOT_FOUND", HttpStatus.NOT_FOUND));
        segregationOfDutiesService.validateRequesterNotApprover(proposal.getCreatedBy(), actor, false);
        try {
            proposal.approve(actor);
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage(), "PAYMENT_PROPOSAL_STATE_INVALID", HttpStatus.CONFLICT);
        }
        VendorPaymentProposal saved = repository.save(proposal);
        auditService.record("APPROVE", "VENDOR_PAYMENT_PROPOSAL", saved.getId(), actor,
                "{\"createdBy\":\"" + saved.getCreatedBy() + "\"}", null);
        return result(saved, allocations(saved));
    }

    @Transactional
    public ProposalResult executeProposal(String id, String operationId, String paymentMethod, String actor) {
        VendorPaymentProposal proposal = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessRuleException("Payment proposal not found", "PAYMENT_PROPOSAL_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (proposal.getStatus() == VendorPaymentProposal.Status.EXECUTED) {
            if (operationId.equals(proposal.getOperationId())) return result(proposal, allocations(proposal));
            throw new BusinessRuleException("Payment proposal was already executed", "PAYMENT_PROPOSAL_ALREADY_EXECUTED", HttpStatus.CONFLICT);
        }
        if (proposal.getStatus() != VendorPaymentProposal.Status.APPROVED) {
            throw new BusinessRuleException("Payment proposal must be approved before execution", "PAYMENT_PROPOSAL_APPROVAL_REQUIRED", HttpStatus.CONFLICT);
        }
        segregationOfDutiesService.validatePreparerNotDisburser(proposal.getCreatedBy(), actor,
                proposal.getProposedAmount(), BigDecimal.ZERO);
        segregationOfDutiesService.validateCreatorNotPoster(proposal.getApprovedBy(), actor, "payment disbursement");
        List<VendorPaymentProposalAllocation> allocations = allocations(proposal);
        List<ProcurementApi.SupplierPaymentPayload> paymentPayloads = new java.util.ArrayList<>();
        for (int index = 0; index < allocations.size(); index++) {
            VendorPaymentProposalAllocation allocation = allocations.get(index);
            String paymentOperationId = operationId + ":" + (index + 1);
            paymentPayloads.add(new ProcurementApi.SupplierPaymentPayload(
                    "AUTO", System.currentTimeMillis(), proposal.getSupplierId(), allocation.getInvoiceId(),
                    allocation.getAmount(), paymentMethod, "Payment proposal " + proposal.getProposalNumber(), paymentOperationId));
        }
        List<ProcurementApi.SupplierPaymentResponse> payments = procurementService.createSupplierPaymentsForProposal(paymentPayloads);
        String firstPaymentId = null;
        for (int index = 0; index < allocations.size(); index++) {
            VendorPaymentProposalAllocation allocation = allocations.get(index);
            ProcurementApi.SupplierPaymentResponse payment = payments.get(index);
            allocation.linkPayment(payment.id(), payment.operationId());
            allocationRepository.save(allocation);
            if (firstPaymentId == null) firstPaymentId = payment.id();
        }
        proposal.execute(operationId, firstPaymentId, actor);
        VendorPaymentProposal saved = repository.save(proposal);
        auditService.record("EXECUTE", "VENDOR_PAYMENT_PROPOSAL", saved.getId(), actor,
                "{\"approvedBy\":\"" + saved.getApprovedBy() + "\",\"operationId\":\""
                        + operationId + "\",\"paymentCount\":" + allocations.size() + "}", null);
        return result(saved, allocations);
    }

    @Transactional(readOnly = true)
    public List<ProposalResult> getProposalsForSupplier(String supplierId) {
        return repository.findBySupplierId(supplierId).stream().map(this::result).toList();
    }

    @Transactional(readOnly = true)
    public List<ProposalResult> getProposals() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::result).toList();
    }

    private ValidatedAllocations validateAllocations(String supplierId, List<AllocationInput> requested) {
        if (requested == null || requested.isEmpty()) {
            throw rule("A payment proposal requires at least one invoice allocation", "PAYMENT_PROPOSAL_ALLOCATIONS_REQUIRED");
        }
        Map<String, AllocationInput> unique = new LinkedHashMap<>();
        String currencyCode = null;
        for (AllocationInput allocation : requested) {
            if (allocation == null || allocation.invoiceId() == null || allocation.invoiceId().isBlank()
                    || allocation.amount() == null || allocation.amount().signum() <= 0) {
                throw rule("Each payment proposal allocation requires an invoice and positive amount", "PAYMENT_PROPOSAL_ALLOCATION_INVALID");
            }
            if (unique.putIfAbsent(allocation.invoiceId(), allocation) != null) {
                throw rule("An invoice cannot occur twice in one payment proposal", "PAYMENT_PROPOSAL_INVOICE_DUPLICATE");
            }
            SupplierInvoice invoice = supplierInvoiceRepository.findById(allocation.invoiceId())
                    .orElseThrow(() -> rule("Supplier invoice not found", "PROC_INVOICE_NOT_FOUND"));
            if (!Objects.equals(supplierId, invoice.getSupplierId())) {
                throw rule("All proposal invoices must belong to the selected supplier", "PROC_INVOICE_SUPPLIER_MISMATCH");
            }
            if (currencyCode == null) currencyCode = invoice.getCurrencyCode();
            else if (!currencyCode.equals(invoice.getCurrencyCode())) {
                throw rule("All invoices in a payment proposal must use the same currency", "PAYMENT_PROPOSAL_CURRENCY_MISMATCH");
            }
            if (SupplierInvoice.Status.PAID.name().equals(invoice.getStatus())
                    || SupplierInvoice.Status.CANCELLED.name().equals(invoice.getStatus())) {
                throw rule("Paid or cancelled invoices cannot be proposed for payment", "PROC_INVOICE_ALREADY_PAID");
            }
            BigDecimal paid = supplierPaymentRepository.sumPostedAmountBySupplierInvoiceId(invoice.getId());
            BigDecimal outstanding = invoice.getNetAmount().subtract(paid == null ? BigDecimal.ZERO : paid);
            if (allocation.amount().compareTo(outstanding) > 0) {
                throw rule("A proposal allocation exceeds the invoice outstanding balance", "PAYMENT_PROPOSAL_OVERPAYMENT");
            }
        }
        return new ValidatedAllocations(List.copyOf(unique.values()), currencyCode);
    }

    private List<VendorPaymentProposalAllocation> allocations(VendorPaymentProposal proposal) {
        List<VendorPaymentProposalAllocation> persisted = allocationRepository.findByProposalIdOrderByLineNoAsc(proposal.getId());
        if (!persisted.isEmpty()) return persisted;
        return List.of(new VendorPaymentProposalAllocation(
                proposal.getId(), 1, proposal.getInvoiceId(), proposal.getProposedAmount()));
    }

    private ProposalResult result(VendorPaymentProposal proposal) {
        return result(proposal, allocations(proposal));
    }

    private ProposalResult result(VendorPaymentProposal proposal, List<VendorPaymentProposalAllocation> allocations) {
        return new ProposalResult(proposal.getId(), proposal.getProposalNumber(), proposal.getSupplierId(),
                proposal.getInvoiceId(), proposal.getProposedAmount(), proposal.getCurrencyCode(), proposal.getDueDate(), proposal.getStatus(),
                proposal.getCreatedBy(), proposal.getApprovedBy(), proposal.getExecutedBy(), proposal.getOperationId(),
                proposal.getSupplierPaymentId(), proposal.getCreatedAt(), proposal.getUpdatedAt(),
                allocations.stream().map(allocation -> new AllocationResult(allocation.getId(), allocation.getInvoiceId(),
                        allocation.getAmount(), allocation.getSupplierPaymentId(), allocation.getPaymentOperationId())).toList());
    }

    private BusinessRuleException rule(String message, String code) {
        return new BusinessRuleException(message, code, HttpStatus.CONFLICT);
    }

    @Transactional
    public record AllocationInput(String invoiceId, BigDecimal amount) {
    }

    public record AllocationResult(String id, String invoiceId, BigDecimal amount,
                                   String supplierPaymentId, String paymentOperationId) {
    }

    public record ProposalResult(String id, String proposalNumber, String supplierId, String invoiceId,
                                 BigDecimal proposedAmount, String currencyCode, LocalDate dueDate,
                                 VendorPaymentProposal.Status status,
                                 String createdBy, String approvedBy, String executedBy, String operationId,
                                 String supplierPaymentId, long createdAt, long updatedAt,
                                 List<AllocationResult> allocations) {
    }

    private record ValidatedAllocations(List<AllocationInput> allocations, String currencyCode) {
    }
}
