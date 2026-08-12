package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.domain.VendorPaymentProposal;
import com.bemo.hr.trade.procurement.infrastructure.VendorPaymentProposalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.approval.SegregationOfDutiesService;

@Service
public class VendorPaymentProposalService {

    private final VendorPaymentProposalRepository repository;
    private final ProcurementService procurementService;
    private final SegregationOfDutiesService segregationOfDutiesService;

    public VendorPaymentProposalService(VendorPaymentProposalRepository repository, ProcurementService procurementService,
                                        SegregationOfDutiesService segregationOfDutiesService) {
        this.repository = repository;
        this.procurementService = procurementService;
        this.segregationOfDutiesService = segregationOfDutiesService;
    }

    @Transactional
    public VendorPaymentProposal createProposal(String supplierId, String invoiceId, BigDecimal proposedAmount, LocalDate dueDate, String actor) {
        VendorPaymentProposal proposal = new VendorPaymentProposal(supplierId, invoiceId, proposedAmount, dueDate, actor);
        return repository.save(proposal);
    }

    @Transactional
    public VendorPaymentProposal approveProposal(String id, String actor) {
        VendorPaymentProposal proposal = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessRuleException("Payment proposal not found", "PAYMENT_PROPOSAL_NOT_FOUND", HttpStatus.NOT_FOUND));
        segregationOfDutiesService.validateRequesterNotApprover(proposal.getCreatedBy(), actor, false);
        try {
            proposal.approve(actor);
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage(), "PAYMENT_PROPOSAL_STATE_INVALID", HttpStatus.CONFLICT);
        }
        return repository.save(proposal);
    }

    @Transactional
    public VendorPaymentProposal executeProposal(String id, String operationId, String paymentMethod, String actor) {
        VendorPaymentProposal proposal = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessRuleException("Payment proposal not found", "PAYMENT_PROPOSAL_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (proposal.getStatus() == VendorPaymentProposal.Status.EXECUTED) {
            if (operationId.equals(proposal.getOperationId())) return proposal;
            throw new BusinessRuleException("Payment proposal was already executed", "PAYMENT_PROPOSAL_ALREADY_EXECUTED", HttpStatus.CONFLICT);
        }
        if (proposal.getStatus() != VendorPaymentProposal.Status.APPROVED) {
            throw new BusinessRuleException("Payment proposal must be approved before execution", "PAYMENT_PROPOSAL_APPROVAL_REQUIRED", HttpStatus.CONFLICT);
        }
        segregationOfDutiesService.validatePreparerNotDisburser(proposal.getCreatedBy(), actor,
                proposal.getProposedAmount(), BigDecimal.ZERO);
        var payment = procurementService.createSupplierPayment(new ProcurementApi.SupplierPaymentPayload(
                "AUTO", System.currentTimeMillis(), proposal.getSupplierId(), proposal.getInvoiceId(),
                proposal.getProposedAmount(), paymentMethod, "Payment proposal " + proposal.getProposalNumber(), operationId));
        proposal.execute(operationId, payment.id(), actor);
        return repository.save(proposal);
    }

    @Transactional(readOnly = true)
    public List<VendorPaymentProposal> getProposalsForSupplier(String supplierId) {
        return repository.findBySupplierId(supplierId);
    }

    @Transactional(readOnly = true)
    public List<VendorPaymentProposal> getProposals() {
        return repository.findAllByOrderByCreatedAtDesc();
    }
}
